package com.bst.server.modules.tax.services;

import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.tax.data.dtos.TaxRateConfigResponse;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import org.springframework.http.ResponseEntity;
import com.bst.server.modules.tax.data.dtos.TaxRateConfigRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.UUID;

public interface TaxRateConfigService {
    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

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
     */
    ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> create(
            TaxRateConfigRequest.Create request,
            WebRequest webRequest
    );

    /**
     * Get a single tax rate configuration by its UUID.
     * <p>
     * Rules:
     * - Only non-deleted records are returned
     * - Soft-deleted records throw NOT_FOUND
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException  (id not found or record is deleted)
     */
    ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> getById(
            UUID id,
            WebRequest webRequest
    );

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
     */
    ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> update(
            UUID id,
            TaxRateConfigRequest.Update request,
            WebRequest webRequest
    );

    // =========================================================================
    // DELETE & RESTORE
    // =========================================================================

    /**
     * Soft delete a tax rate configuration.
     * <p>
     * Rules:
     * - Sets deleted = true and isActive = false on the record
     * - Record is excluded from all lookups and calculations after soft delete
     * - Preserves complete audit history (createdAt, updatedAt untouched)
     * - Cannot soft delete an already-deleted record
     * <p>
     * Throws:
     * - TaxRateConfigNotFoundException           (id not found or already deleted)
     * - TaxRateConfigOperationNotAllowedException (active applications referencing this config)
     */
    ResponseEntity<CustomResponse<Void>> softDelete(
            UUID id,
            WebRequest webRequest
    );

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
     */
    ResponseEntity<CustomResponse<Void>> hardDelete(
            UUID id,
            WebRequest webRequest
    );

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
     */
    ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> restore(
            UUID id,
            WebRequest webRequest
    );

    // =========================================================================
    // ENABLE / DISABLE
    // =========================================================================

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
     */
    ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> enable(
            UUID id,
            WebRequest webRequest
    );

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
     */
    ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> disable(
            UUID id,
            WebRequest webRequest
    );

    // =========================================================================
    // SEARCH & LISTING
    // =========================================================================

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
     */
    ResponseEntity<CustomResponse<PagedResponse<TaxRateConfigResponse.Summary>>> search(
            TaxRateConfigRequest.Search request,
            WebRequest webRequest
    );

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
     */
    ResponseEntity<CustomResponse<List<TaxRateConfigResponse.Summary>>> getAllActive(
            TaxModeEnum taxType,
            WebRequest webRequest
    );

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
     */
    ResponseEntity<CustomResponse<Integer>> bulkHardDelete(
            TaxRateConfigRequest.BulkDelete request,
            WebRequest webRequest
    );
}
