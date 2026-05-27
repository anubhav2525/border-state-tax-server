package com.bst.server.modules.tax.controllers;

import com.bst.server.common.constants.ApiVersion;
import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.tax.data.dtos.TaxRateConfigRequest;
import com.bst.server.modules.tax.data.dtos.TaxRateConfigResponse;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import com.bst.server.modules.tax.services.TaxRateConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Tax Rate Configuration management.
 *
 * <p>Base URL: /api/v1/tax-rate-configs
 *
 * <p>Access Control:
 * <ul>
 *   <li>SUPER_ADMIN — full access (all endpoints)
 *   <li>ADMIN       — read, create, update, enable/disable, soft delete, bulk ops
 *   <li>AGENT       — read-only (getById, search, getAllActive)
 *   <li>USER        — getAllActive only (for application form dropdowns)
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping(ApiVersion.V1 + "/tax-rate-configs")
@RequiredArgsConstructor
public class TaxRateConfigController {

    private final TaxRateConfigService taxRateConfigService;

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * POST /api/v1/tax-rate-configs
     *
     * <p>Create a new tax rate configuration.
     *
     * <p>Validations enforced:
     * <ul>
     *   <li>stateName + vehicleSeating + taxType must be unique among non-deleted records
     *   <li>At least one payment mode must have a non-zero rate or commission
     *   <li>All rate and commission values must be >= 0
     *   <li>vehicleSeating format: digit(s)+1 (e.g. 4+1, 7+1, 12+1)
     * </ul>
     *
     * <p>Returns: 201 CREATED with full Detail response
     *
     * @param request    validated create payload
     * @param webRequest for response metadata (path, timestamp)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('TAX_RATE:CREATE')")
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> create(
            @Valid @RequestBody TaxRateConfigRequest.Create request,
            WebRequest webRequest
    ) {
        log.info("POST /api/v1/tax-rate-configs — stateName={}, vehicleSeating={}, taxType={}",
                request.getStateName(), request.getVehicleSeating(), request.getTaxType());

        return taxRateConfigService.create(request, webRequest);
    }

    // =========================================================================
    // GET BY ID
    // =========================================================================

    /**
     * GET /api/v1/tax-rate-configs/{id}
     *
     * <p>Fetch full detail of a single tax rate config by UUID.
     *
     * <p>Rules:
     * <ul>
     *   <li>Soft-deleted records return 404
     *   <li>Returns complete rate + commission + computed total for all modes
     *   <li>Returns availability flag per mode (true if rate > 0 OR commission > 0)
     * </ul>
     *
     * <p>Returns: 200 OK with Detail response
     *
     * @param id UUID of the tax rate config
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TAX_RATE:READ')")
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> getById(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("GET /api/v1/tax-rate-configs/{}", id);

        return taxRateConfigService.getById(id, webRequest);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    /**
     * PATCH /api/v1/tax-rate-configs/{id}
     *
     * <p>Partially update an existing tax rate configuration.
     *
     * <p>Rules:
     * <ul>
     *   <li>Only rate and commission fields are updatable
     *   <li>stateName, vehicleSeating, taxType are immutable — they form the natural key
     *   <li>Only non-null fields in the request body are applied
     *   <li>At least one field must be provided (empty body returns 422)
     *   <li>After update, at least one mode must still have a non-zero value
     *   <li>Soft-deleted records cannot be updated — restore first
     * </ul>
     *
     * <p>Returns: 200 OK with updated Detail response
     *
     * @param id      UUID of the tax rate config to update
     * @param request partial update payload (all fields optional)
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('TAX_RATE:UPDATE')")
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Detail>> update(
            @PathVariable UUID id,
            @Valid @RequestBody TaxRateConfigRequest.Update request,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/tax-rate-configs/{}", id);

        return taxRateConfigService.update(id, request, webRequest);
    }

    // =========================================================================
    // SEARCH
    // =========================================================================

    /**
     * GET /api/v1/tax-rate-configs/search
     *
     * <p>Paginated, filterable, sortable search across all tax rate configs.
     *
     * <p>Supported query params (all optional):
     * <ul>
     *   <li>stateName      — partial case-insensitive match
     *   <li>vehicleSeating — exact match (e.g. "4+1")
     *   <li>taxType        — exact enum match (BORDER / ROAD)
     *   <li>enabled        — true / false
     *   <li>page           — 0-based page index (default: 0)
     *   <li>size           — page size (default: 20, max: 100)
     *   <li>sortBy         — field name (default: createdAt)
     *   <li>sortDir        — asc / desc (default: desc)
     * </ul>
     *
     * <p>Note: deleted records are always excluded from search results.
     *
     * <p>Returns: 200 OK with paginated Summary list
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('TAX_RATE:READ')")
    public ResponseEntity<CustomResponse<PagedResponse<TaxRateConfigResponse.Summary>>> search(
            @Valid @RequestParam TaxRateConfigRequest.Search request,
            WebRequest webRequest
    ) {
        log.info("GET /api/v1/tax-rate-configs/search — page={}, size={}, state={}, taxType={}",
                request.getPage(), request.getSize(),
                request.getStateName(), request.getTaxType());

        return taxRateConfigService.search(request, webRequest);
    }

    // =========================================================================
    // GET ALL ACTIVE  (dropdown / permit form support)
    // =========================================================================

    /**
     * GET /api/v1/tax-rate-configs/active
     *
     * <p>Returns all active (isActive = true, deleted = false) configurations.
     * Intended for UI dropdowns and permit application forms.
     *
     * <p>Optional query param:
     * <ul>
     *   <li>taxType — filter results to a specific mode (BORDER or ROAD)
     *                 if omitted, all active configs are returned
     * </ul>
     *
     * <p>Returns: 200 OK with lightweight Summary list (no commission breakdown)
     *
     * @param taxType optional TaxModeEnum filter
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('TAX_RATE:READ')")
    public ResponseEntity<CustomResponse<List<TaxRateConfigResponse.Summary>>> getAllActive(
            @RequestParam(required = false) TaxModeEnum taxType,
            WebRequest webRequest
    ) {
        log.info("GET /api/v1/tax-rate-configs/active - taxType filter={}", taxType);

        return taxRateConfigService.getAllActive(taxType, webRequest);
    }

    // =========================================================================
    // ENABLE
    // =========================================================================

    /**
     * PATCH /api/v1/tax-rate-configs/{id}/enable
     *
     * <p>Enable a tax rate configuration (set isActive = true).
     *
     * <p>Rules:
     * <ul>
     *   <li>Soft-deleted records cannot be enabled — restore first
     *   <li>Already-enabled records return 400
     * </ul>
     *
     * <p>Returns: 200 OK with updated Detail response
     *
     * @param id UUID of the config to enable
     */
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('TAX_RATE:UPDATE')")
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> enable(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/tax-rate-configs/{}/enable", id);

        return taxRateConfigService.enable(id, webRequest);
    }

    // =========================================================================
    // DISABLE
    // =========================================================================

    /**
     * PATCH /api/v1/tax-rate-configs/{id}/disable
     *
     * <p>Disable a tax rate configuration (set isActive = false).
     *
     * <p>Rules:
     * <ul>
     *   <li>Disabled configs are excluded from dropdowns and tax calculations
     *   <li>Soft-deleted records cannot be disabled
     *   <li>Already-disabled records return 400
     * </ul>
     *
     * <p>Returns: 200 OK with updated Detail response
     *
     * @param id UUID of the config to disable
     */
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('TAX_RATE:UPDATE')")
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> disable(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/tax-rate-configs/{}/disable", id);

        return taxRateConfigService.disable(id, webRequest);
    }

    // =========================================================================
    // SOFT DELETE
    // =========================================================================

    /**
     * DELETE /api/v1/tax-rate-configs/{id}
     *
     * <p>Soft delete a tax rate configuration.
     *
     * <p>Rules:
     * <ul>
     *   <li>Sets deleted = true and isActive = false
     *   <li>Record is excluded from all lookups, search, and calculations
     *   <li>Can be restored via /restore endpoint
     *   <li>Already-deleted records return 400
     *   <li>Audit timestamps (createdAt, updatedAt) are preserved
     * </ul>
     *
     * <p>Returns: 200 OK with null data body
     *
     * @param id UUID of the config to soft's delete
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TAX_RATE:DELETE')")
    public ResponseEntity<CustomResponse<Void>> softDelete(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("DELETE /api/v1/tax-rate-configs/{} (soft)", id);

        return taxRateConfigService.softDelete(id, webRequest);
    }

    // =========================================================================
    // HARD DELETE
    // =========================================================================

    /**
     * DELETE /api/v1/tax-rate-configs/{id}/hard
     *
     * <p>Permanently delete a tax rate configuration from the database.
     *
     * <p>WARNING:
     * <ul>
     *   <li>Irreversible — no restore possible
     *   <li>Restricted to SUPER_ADMIN only
     *   <li>Should only be used to clean up test/seed data
     *   <li>Do NOT use if any application references this config ID
     * </ul>
     *
     * <p>Returns: 200 OK with null data body
     *
     * @param id UUID of the config to permanently delete
     */
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CustomResponse<Void>> hardDelete(
            @PathVariable(name = "id") UUID id,
            WebRequest webRequest
    ) {
        log.warn("DELETE /api/v1/tax-rate-configs/{}/hard — PERMANENT DELETE by admin", id);

        return taxRateConfigService.hardDelete(id, webRequest);
    }

    // =========================================================================
    // RESTORE
    // =========================================================================

    /**
     * PATCH /api/v1/tax-rate-configs/{id}/restore
     *
     * <p>Restore a previously soft-deleted tax rate configuration.
     *
     * <p>Rules:
     * <ul>
     *   <li>Sets deleted = false and isActive = true
     *   <li>Re-validates uniqueness before restoring
     *       (a new record with the same combination may have been created
     *       while this one was deleted — in that case, restore is rejected)
     *   <li>Only works on soft-deleted records (non-deleted records return 400)
     * </ul>
     *
     * <p>Returns: 200 OK with restored Detail response
     *
     * @param id UUID of the soft-deleted config to restore
     */
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('TAX_RATE:UPDATE')")
    public ResponseEntity<CustomResponse<TaxRateConfigResponse.Summary>> restore(
            @PathVariable(name = "id") UUID id,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/tax-rate-configs/{}/restore", id);

        return taxRateConfigService.restore(id, webRequest);
    }

    // =========================================================================
    // BULK HARD DELETE
    // =========================================================================

    /**
     * DELETE /api/v1/tax-rate-configs/bulk/hard
     *
     * <p>Permanently delete multiple tax rate configurations in one call.
     *
     * <p>Rules:
     * <ul>
     *   <li>All provided IDs must exist — any missing ID fails the entire operation
     *   <li>Entire operation is transactional — partial deletes never happen
     *   <li>Restricted to SUPER_ADMIN only
     * </ul>
     *
     * <p>Request body:
     * <pre>{@code
     * {
     *   "ids": ["uuid-1", "uuid-2", "uuid-3"]
     * }
     * }</pre>
     *
     * <p>Returns: 200 OK with count of deleted records
     *
     * @param request list of UUIDs to permanently delete
     */
    @DeleteMapping("/bulk/hard")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CustomResponse<Integer>> bulkHardDelete(
            @Valid @RequestBody TaxRateConfigRequest.BulkDelete request,
            WebRequest webRequest
    ) {
        log.warn("DELETE /api/v1/tax-rate-configs/bulk/hard — ids={}", request.getIds());

        return taxRateConfigService.bulkHardDelete(request, webRequest);
    }
}
