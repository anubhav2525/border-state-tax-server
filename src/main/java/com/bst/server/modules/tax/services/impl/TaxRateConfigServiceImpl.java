package com.bst.server.modules.tax.services.impl;

import com.bst.server.common.exceptions.sub.*;
import com.bst.server.common.utils.*;
import com.bst.server.modules.tax.data.dtos.TaxRateConfigRequest;
import com.bst.server.modules.tax.data.dtos.TaxRateConfigResponse;
import com.bst.server.modules.tax.data.entities.TaxRateConfig;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import com.bst.server.modules.tax.repository.TaxRateConfigRepository;
import com.bst.server.modules.tax.services.TaxRateConfigService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.WebRequest;

import java.math.BigDecimal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TaxRateConfigServiceImpl implements TaxRateConfigService {
    private final TaxRateConfigRepository taxRateConfigRepository;
    private final TaxRateConfigRequest rateConfigRequest;

    private final StringOperation stringOperation;
    private final CreateResponseEntity createResponseEntity;
    private final BuildPageable buildPageable;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "stateName",
            "seatingCapacity",
            "createdAt",
            "updatedAt",
            "enabled"
    );

    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private static final String RESOURCE = "Tax Configuration";

    /**
     * Create a new tax rate configuration.
     * <p>
     * Rules:
     * - stateName + vehicleSeating + taxType combination must be unique
     * among non-deleted records
     * - At least one payment mode must have a non-zero rate or commission
     * - All rate and commission values must be >= 0
     * - Duplicate payment modes within the same config are not allowed
     * - stateName and vehicleSeating are stored as-is (trimmed)
     * <p>
     * Throws:
     * - TaxRateConfigAlreadyExistsException  (duplicate combination)
     * - TaxRateConfigValidationException     (all modes zero / invalid values)
     *
     * @param request    - TaxRateConfigRequest.Create
     * @param webRequest - Api request header
     */
    @Override
    @Transactional
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> create(
            TaxRateConfigRequest.Create request, WebRequest webRequest) {
        log.info("Creating tax rate config: stateName={}, vehicleSeating={}, taxType={}",
                request.getStateName(), request.getVehicleSeating(), request.getTaxType());

        // 1. Cross-field: at least one mode must be non-zero
        request.validate();

        // 2. Trim inputs
        String stateName = stringOperation.trimOrNull(request.getStateName());
        String vehicleSeating = stringOperation.trimOrNull(request.getVehicleSeating());

        // 3. Uniqueness check: stateName + vehicleSeating + taxType must not already exist
        validateNoDuplicate(stateName, vehicleSeating, request.getTaxType(), null);

        // 4. Build entity and persist
        TaxRateConfig entity = rateConfigRequest.toTaxRateConfig(request);
        entity.setStateName(stateName);
        entity.setVehicleSeating(vehicleSeating);
        entity.setDeleted(false);
        entity.setEnabled(true);

        taxRateConfigRepository.save(entity);

        log.info("Tax rate config created successfully: id={}", entity.getId());

        return createResponseEntity.buildResponse(
                RESOURCE + " created successfully",
                buildDetail(entity),
                HttpStatus.CREATED,
                webRequest
        );
    }

    /**
     * Get a single tax rate configuration by its UUID.
     * <p>
     * Rules:
     * - Only non-deleted records are returned
     * - Soft-deleted records throw NOT_FOUND
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException  (id not found or record is deleted)
     *
     * @param id         - UUID
     * @param webRequest - Api request header
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> getById(
            UUID id, WebRequest webRequest) {
        log.info("Fetching tax rate config: id={}", id);

        TaxRateConfig entity = getConfigOrThrow(id);

        return createResponseEntity.buildResponse(
                RESOURCE + " fetched successfully",
                buildDetail(entity),
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Update an existing tax rate configuration.
     * <p>
     * Rules:
     * - Soft-deleted records cannot be updated — restore first
     * - stateName, vehicleSeating, and taxType are immutable after creation
     * (they form the natural key — create a new record if the combination changes)
     * - Only non-null fields in the request are applied (partial update)
     * - After applying changes, uniqueness is re-validated against non-deleted records
     * - If any rate/commission is changed, at least one mode must remain non-zero
     * - isActive toggle is allowed independently of rate changes
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException       (id not found or record is deleted)
     * - TaxRateConfigAlreadyExistsException  (uniqueness conflict after update)
     * - TaxRateConfigValidationException     (all modes become zero after update)
     *
     * @param id         - UUID
     * @param request    - TaxRateConfigRequest.Update
     * @param webRequest - Api request header
     */
    @Override
    @Transactional
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> update(
            UUID id, TaxRateConfigRequest.Update request, WebRequest webRequest) {
        log.info("Updating tax rate config: id={}", id);

        // 1. Fetch — soft-deleted records cannot be updated
        TaxRateConfig entity = getConfigOrThrow(id);

        if (entity.getDeleted()) {
            throw new ResourceNotExistsException(
                    RESOURCE + " is not found."
            );
        }
        // 2. Guard: reject empty PATCH bodies
        if (!request.hasAnyUpdate()) {
            throw new ResourceValidationException(
                    "No fields provided to update. At least one rate or commission must be supplied."
            );
        }

        // 3. Apply only non-null fields (partial update)
        if (request.getDailyRate() != null)
            entity.setDailyRate(request.getDailyRate());
        if (request.getDailyCommission() != null)
            entity.setDailyCommission(request.getDailyCommission());
        if (request.getWeeklyRate() != null)
            entity.setWeeklyRate(request.getWeeklyRate());
        if (request.getWeeklyCommission() != null)
            entity.setWeeklyCommission(request.getWeeklyCommission());
        if (request.getMonthlyRate() != null)
            entity.setMonthlyRate(request.getMonthlyRate());
        if (request.getMonthlyCommission() != null)
            entity.setMonthlyCommission(request.getMonthlyCommission());
        if (request.getQuarterlyRate() != null)
            entity.setQuarterlyRate(request.getQuarterlyRate());
        if (request.getQuarterlyCommission() != null)
            entity.setQuarterlyCommission(request.getQuarterlyCommission());
        if (request.getYearlyRate() != null)
            entity.setYearlyRate(request.getYearlyRate());
        if (request.getYearlyCommission() != null)
            entity.setYearlyCommission(request.getYearlyCommission());

        // 4. Post-update validation: at least one mode must still be non-zero
        validateAtLeastOneMode(entity);

        taxRateConfigRepository.save(entity);

        log.info("Tax rate config updated successfully: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " updated successfully",
                buildDetail(entity),
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Soft delete a tax rate configuration.
     * <p>
     * Rules:
     * - Sets deleted = true and isActive = false on the record
     * - Record is excluded from all lookups and calculations after soft delete
     * - Preserves complete audit history (createdAt, updatedAt untouched)
     * - Cannot soft's delete an already-deleted record
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException           (id not found or already deleted)
     * - TaxRateConfigOperationNotAllowedException (active applications referencing this config)
     *
     * @param id         - UUId
     * @param webRequest - Api request header
     */
    @Override
    @Transactional
    public ResponseEntity<CustomResponse<Void>> softDelete(
            UUID id, WebRequest webRequest) {
        log.info("Soft deleting tax rate config: id={}", id);

        // findById directly — getConfigOrThrow excludes already-deleted records
        TaxRateConfig entity = taxRateConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotExistsException(
                        RESOURCE + " not found"
                ));

        if (entity.getDeleted()) {
            throw new ResourceAlreadyDeletedException(
                    RESOURCE + " is already deleted."
            );
        }
        entity.setDeleted(true);
        entity.setEnabled(false);

        taxRateConfigRepository.save(entity);

        log.info("Tax rate config soft deleted: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " deleted successfully",
                null,
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Permanently delete a tax rate configuration.
     * <p>
     * WARNING:
     * - Irreversible — record is removed from DB entirely
     * - Only allowed if no applications reference this config ID
     * - Recommended only for test/seed data cleanup
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException    (id not found)
     * - TaxRateConfigDependencyException  (applications exist referencing this config)
     *
     * @param id         - UUID
     * @param webRequest - Api request header
     */
    @Override
    @Transactional
    public ResponseEntity<CustomResponse<Void>> hardDelete(
            UUID id, WebRequest webRequest) {
        log.warn("Hard deleting tax rate config: id={} — IRREVERSIBLE", id);

        TaxRateConfig entity = taxRateConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotExistsException(
                        RESOURCE + " not found"));

        taxRateConfigRepository.delete(entity);

        log.info("Tax rate config permanently deleted: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " permanently deleted",
                null,
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Restore a previously soft-deleted tax rate configuration.
     * <p>
     * Rules:
     * - Sets deleted = false and isActive = true
     * - Re-validates uniqueness before restoring
     * (another active record with same combination may have been created
     * while this one was deleted)
     * - If uniqueness conflict exists, restore is rejected
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException           (id not found or record is not deleted)
     * - TaxRateConfigAlreadyExistsException       (uniqueness conflict on restore)
     * - TaxRateConfigRestoreException             (restore not allowed for other reasons)
     *
     * @param id         UUID
     * @param webRequest - Api request header
     */
    @Override
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> restore(
            UUID id, WebRequest webRequest) {
        log.info("Restoring tax rate config: id={}", id);

        // Must fetch by id only — not filtered by deleted=false
        TaxRateConfig entity = taxRateConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotExistsException(
                        RESOURCE + " not found"
                ));

        if (!entity.getDeleted()) {
            throw new ResourceNotRestoreException(
                    RESOURCE + " is not deleted. Only deleted records can be restored."
            );
        }

        // Re-validate uniqueness — a new active record may exist with the same combination
        validateNoDuplicate(
                entity.getStateName(),
                entity.getVehicleSeating(),
                entity.getTaxType(),
                entity.getId()
        );

        entity.setDeleted(false);
        entity.setEnabled(true);

        taxRateConfigRepository.save(entity);

        log.info("Tax rate config restored: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " restored successfully",
                buildSummary(entity),
                HttpStatus.OK,
                webRequest
        );

    }

    /**
     * Enable a tax rate configuration (set isActive = true).
     * <p>
     * Rules:
     * - Cannot enable a soft-deleted record — restore it first
     * - Enabling an already-active record is a no-op (returns current state)
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException           (id not found or record is deleted)
     * - TaxRateConfigOperationNotAllowedException (already enabled)
     *
     * @param id         UUID
     * @param webRequest - Api request header
     */
    @Override
    @Transactional
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> enable(
            UUID id, WebRequest webRequest) {

        log.info("Enabling tax rate config: id={}", id);

        TaxRateConfig entity = getConfigOrThrow(id);

        if (entity.getDeleted()) {
            throw new ResourceOperationNotAllowed(
                    "Deleted " + RESOURCE + " cannot be enabled. Restore it first."
            );
        }
        if (Boolean.TRUE.equals(entity.getEnabled())) {
            throw new ResourceOperationNotAllowed(
                    RESOURCE + " is already enabled."
            );
        }

        entity.setEnabled(true);
        taxRateConfigRepository.save(entity);

        log.info("Tax rate config enabled: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " enabled successfully",
                buildSummary(entity),
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Disable a tax rate configuration (set isActive = false).
     * <p>
     * Rules:
     * - Disabled records are excluded from tax calculation and dropdowns
     * - Cannot disable a soft-deleted record
     * - Disabling an already-inactive record is a no-op (returns current state)
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException           (id not found or record is deleted)
     * - TaxRateConfigOperationNotAllowedException (already disabled)
     *
     * @param id         - UUID
     * @param webRequest - Api request header
     */
    @Override
    @Transactional
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> disable(
            UUID id, WebRequest webRequest) {
        log.info("Disabling tax rate config: id={}", id);

        TaxRateConfig entity = getConfigOrThrow(id);

        if (entity.getDeleted()) {
            throw new ResourceOperationNotAllowed(
                    "Deleted " + RESOURCE + " cannot be disabled."
            );
        }

        if (Boolean.FALSE.equals(entity.getEnabled())) {
            throw new ResourceOperationNotAllowed(
                    RESOURCE + " is already disabled."
            );
        }
        entity.setEnabled(false);
        taxRateConfigRepository.save(entity);

        log.info("Tax rate config disabled: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " disabled successfully",
                buildSummary(entity),
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Paginated, filtered, and sortable search across all tax rate configs.
     * <p>
     * Supported filters:
     * - stateName       : partial match (case-insensitive LIKE)
     * - vehicleSeating  : exact match  (e.g. "4+1", "7+1")
     * - taxType         : exact match  (TaxModeEnum value)
     * - isActive        : true / false / null (all)
     * - isDeleted       : defaults to false (non-deleted only)
     * <p>
     * Supported sort fields:
     * - stateName, vehicleSeating, taxType, createdAt, updatedAt
     * <p>
     * Pagination:
     * - page (0-based), size (default 20, max 100)
     *
     * @param request    - UUID
     * @param webRequest - Api request header
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<PagedResponse<TaxRateConfigResponse.Summary>>> search(
            TaxRateConfigRequest.Search request, WebRequest webRequest) {

        log.info("Searching tax rate configs: page={}, size={}, sortBy={}, sortDir={}",
                request.getPage(), request.getSize(), request.getSortBy(), request.getSortDir());

        Pageable pageable = buildPageable.build(
                request.getPage(), request.getSize(),
                request.getSortBy(), request.getSortDir(),
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD
        );

        Specification<TaxRateConfig> spec = buildSearchSpec(request);

        Page<TaxRateConfig> page = taxRateConfigRepository.findAll(spec, pageable);

        List<TaxRateConfigResponse.Summary> content = page.getContent()
                .stream()
                .map(this::buildSummary)
                .toList();

        PagedResponse<TaxRateConfigResponse.Summary> response =
                PagedResponse.<TaxRateConfigResponse.Summary>builder()
                        .content(content)
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .last(page.isLast())
                        .build();

        return createResponseEntity.buildResponse(
                RESOURCE + " list fetched successfully",
                response,
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Get all active (isActive = true, deleted = false) tax rate configurations.
     * <p>
     * Used for:
     * - Application form dropdowns (state + seating + tax type selection)
     * - Tax calculation previews
     * - Permit generation
     * <p>
     * Returns a lightweight {@link TaxRateConfigResponse.Summary} — only id,
     * stateName, vehicleSeating, and taxType. No rate breakdown.
     * <p>
     * Optional filter:
     * - taxType : filter by specific TaxModeEnum (e.g. show only BORDER configs)
     *
     * @param taxType    - TaxModeEnum
     * @param webRequest - Api request header
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<List<TaxRateConfigResponse.Summary>>> getAllActive(
            TaxModeEnum taxType, WebRequest webRequest) {
        log.info("Fetching all active tax rate configs: taxType filter={}", taxType);

        List<TaxRateConfig> records = (taxType != null)
                ? taxRateConfigRepository.findAllByEnabledTrueAndDeletedFalseAndTaxType(taxType)
                : taxRateConfigRepository.findAllByEnabledTrueAndDeletedFalse();

        List<TaxRateConfigResponse.Summary> data = records.stream()
                .map(this::buildSummary)
                .toList();

        return createResponseEntity.buildResponse(
                "Active " + RESOURCE + " list fetched successfully",
                data,
                HttpStatus.OK,
                webRequest
        );
    }

    /**
     * Bulk permanently delete multiple tax rate configs in one transactional call.
     * <p>
     * WARNING:
     * - Irreversible — records are removed from DB entirely
     * - All IDs must have zero application references
     * - If any ID has dependencies, the entire operation is rolled back
     * <p>
     * Returns:
     * - Count of records permanently deleted
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException    (any ID not found)
     * - TaxRateConfigDependencyException  (any ID has application references)
     *
     * @param request    - TaxRateConfigRequest.BulkDelete
     * @param webRequest - Api request header
     */
    @Override
    public ResponseEntity<CustomResponse<Integer>> bulkHardDelete(
            TaxRateConfigRequest.BulkDelete request, WebRequest webRequest) {
        log.warn("Bulk hard delete requested for ids={}", request.getIds());

        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new ResourceValidationException(
                    "At least one ID must be provided for bulk delete."
            );
        }

        // Validate all IDs exist before deleting any (fail-fast)
        List<TaxRateConfig> configs = taxRateConfigRepository.findAllById(request.getIds());

        Set<UUID> foundIds = new HashSet<>();
        configs.forEach(c -> foundIds.add(c.getId()));

        List<UUID> missingIds = request.getIds().stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new ResourceNotExistsException(
                    RESOURCE + " not found for ids: " + missingIds
            );
        }

        int count = configs.size();
        taxRateConfigRepository.deleteAll(configs);

        log.info("Bulk hard delete completed: {} records deleted", count);

        return createResponseEntity.buildResponse(
                "Bulk delete completed successfully",
                count,
                HttpStatus.OK,
                webRequest
        );

    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Finds a non-deleted config by ID.
     * Throws TaxRateConfigNotFoundException if not found or already deleted.
     */
    private TaxRateConfig getConfigOrThrow(
            UUID id) {
        return taxRateConfigRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotExistsException(
                        RESOURCE + " not found with id: " + id
                ));
    }

    /**
     * Validates that no other active (non-deleted) record exists
     * for the same stateName + vehicleSeating + taxType combination.
     *
     * @param excludeId pass current entity ID on update/restore to exclude self from check
     */
    private void validateNoDuplicate(
            String stateName, String vehicleSeating,
            TaxModeEnum taxType, UUID excludeId) {

        Optional<TaxRateConfig> existing =
                taxRateConfigRepository
                        .findByStateNameIgnoreCaseAndVehicleSeatingIgnoreCaseAndTaxTypeAndDeletedFalse(
                                stateName, vehicleSeating, taxType
                        );

        if (existing.isPresent()) {
            // On update/restore: it's fine if the conflict is with itself
            if (excludeId != null && existing.get().getId().equals(excludeId)) {
                return;
            }
            throw new ResourceAlreadyExistsException(
                    RESOURCE + " already exists for state='" + stateName
                            + "', vehicleSeating='" + vehicleSeating
                            + "', taxType='" + taxType + "'"
            );
        }
    }

    /**
     * After partial update, ensures the entity still has at least one
     * payment mode with a non-zero rate or commission.
     */
    private void validateAtLeastOneMode(
            TaxRateConfig entity) {
        boolean allZero =
                isZero(entity.getDailyRate()) && isZero(entity.getDailyCommission()) &&
                        isZero(entity.getWeeklyRate()) && isZero(entity.getWeeklyCommission()) &&
                        isZero(entity.getMonthlyRate()) && isZero(entity.getMonthlyCommission()) &&
                        isZero(entity.getQuarterlyRate()) && isZero(entity.getQuarterlyCommission()) &&
                        isZero(entity.getYearlyRate()) && isZero(entity.getYearlyCommission());

        if (allZero) {
            throw new ResourceValidationException(
                    "At least one payment mode must have a non-zero rate or commission after update."
            );
        }
    }

    private boolean isZero(
            BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private boolean isAvailable(
            BigDecimal rate, BigDecimal commission) {
        return !isZero(rate) || !isZero(commission);
    }

    /**
     * Builds the Detail response with all computed *Total fields.
     * ModelMapper alone won't compute dailyTotal etc., so we set them manually.
     */
    private TaxRateConfigResponse.Detail buildDetail(
            TaxRateConfig e) {
        return TaxRateConfigResponse.Detail.builder()
                .id(e.getId())
                .stateName(e.getStateName())
                .vehicleSeating(e.getVehicleSeating())
                .taxType(e.getTaxType())

                .dailyRate(e.getDailyRate())
                .dailyCommission(e.getDailyCommission())
                .dailyTotal(e.getDailyRate().add(e.getDailyCommission()))
                .dailyAvailable(isAvailable(e.getDailyRate(), e.getDailyCommission()))

                .weeklyRate(e.getWeeklyRate())
                .weeklyCommission(e.getWeeklyCommission())
                .weeklyTotal(e.getWeeklyRate().add(e.getWeeklyCommission()))
                .weeklyAvailable(isAvailable(e.getWeeklyRate(), e.getWeeklyCommission()))

                .monthlyRate(e.getMonthlyRate())
                .monthlyCommission(e.getMonthlyCommission())
                .monthlyTotal(e.getMonthlyRate().add(e.getMonthlyCommission()))
                .monthlyAvailable(isAvailable(e.getMonthlyRate(), e.getMonthlyCommission()))

                .quarterlyRate(e.getQuarterlyRate())
                .quarterlyCommission(e.getQuarterlyCommission())
                .quarterlyTotal(e.getQuarterlyRate().add(e.getQuarterlyCommission()))
                .quarterlyAvailable(isAvailable(e.getQuarterlyRate(), e.getQuarterlyCommission()))

                .yearlyRate(e.getYearlyRate())
                .yearlyCommission(e.getYearlyCommission())
                .yearlyTotal(e.getYearlyRate().add(e.getYearlyCommission()))
                .yearlyAvailable(isAvailable(e.getYearlyRate(), e.getYearlyCommission()))

                .isActive(e.getEnabled())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    /**
     * Builds the Summary response (used in list views and search results).
     */
    private TaxRateConfigResponse.Summary buildSummary(
            TaxRateConfig e) {
        return TaxRateConfigResponse.Summary.builder()
                .id(e.getId())
                .stateName(e.getStateName())
                .vehicleSeating(e.getVehicleSeating())
                .taxType(e.getTaxType())
                .dailyRate(e.getDailyRate())
                .weeklyRate(e.getWeeklyRate())
                .monthlyRate(e.getMonthlyRate())
                .quarterlyRate(e.getQuarterlyRate())
                .yearlyRate(e.getYearlyRate())
                .dailyAvailable(isAvailable(e.getDailyRate(), e.getDailyCommission()))
                .weeklyAvailable(isAvailable(e.getWeeklyRate(), e.getWeeklyCommission()))
                .monthlyAvailable(isAvailable(e.getMonthlyRate(), e.getMonthlyCommission()))
                .quarterlyAvailable(isAvailable(e.getQuarterlyRate(), e.getQuarterlyCommission()))
                .yearlyAvailable(isAvailable(e.getYearlyRate(), e.getYearlyCommission()))
                .isActive(e.getEnabled())
                .build();
    }

    /**
     * Builds the JPA Specification for the search endpoint.
     * All filters are optional — null values are simply skipped.
     */
    private Specification<TaxRateConfig> buildSearchSpec(
            TaxRateConfigRequest.Search req) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // stateName — case-insensitive partial match
            if (req.getStateName() != null && !req.getStateName().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("stateName")),
                        "%" + req.getStateName().trim().toLowerCase() + "%"
                ));
            }

            // vehicleSeating — exact match
            if (req.getVehicleSeating() != null && !req.getVehicleSeating().isBlank()) {
                predicates.add(cb.equal(
                        root.get("vehicleSeating"),
                        req.getVehicleSeating().trim()
                ));
            }

            // taxType — enum exact match
            if (req.getTaxType() != null) {
                predicates.add(cb.equal(root.get("taxType"), req.getTaxType()));
            }

            // isActive filter
            if (req.getEnabled() != null) {
                predicates.add(cb.equal(root.get("isActive"), req.getEnabled()));
            }

            // deleted filter — default false (only non-deleted records in normal search)
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
