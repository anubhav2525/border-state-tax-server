package com.bst.server.modules.tax.controllers;

import com.bst.server.common.constants.ApiVersion;
import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.tax.data.dtos.ApplicationRequest;
import com.bst.server.modules.tax.data.dtos.ApplicationResponse;
import com.bst.server.modules.tax.data.enums.AppStatusEnum;
import com.bst.server.modules.tax.services.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

/**
 * REST Controller for Permit Tax Application management.
 *
 * <p>Base URL: /api/v1/applications
 *
 * <p>Access Control:
 * <ul>
 *   <li>SUPER_ADMIN — full access (all endpoints)
 *   <li>ADMIN       — review, refund, search, mark complete, analytics
 *   <li>AGENT       — read, search, mark as read
 *   <li>USER        — create draft, submit, cancel own application
 * </ul>
 *
 * <p>Application Lifecycle:
 * <pre>
 * DRAFT -> SUBMITTED -> PAID -> COMPLETED
 *                \-> CANCELLED
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping(ApiVersion.V1 + "/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // =========================================================================
    // CALCULATE + CREATE DRAFT
    // =========================================================================

    /**
     * POST /api/v1/applications/calculate
     *
     * <p>Calculate tax amount and create a DRAFT application.
     *
     * <p>Rules:
     * <ul>
     *   <li>Valid active TaxRateConfig must exist</li>
     *   <li>Selected paymentMode must have non-zero rate</li>
     *   <li>Tax amount, commission, and total are snapshotted</li>
     *   <li>Duration and endDate are auto-computed</li>
     *   <li>Application is stored with DRAFT status</li>
     * </ul>
     *
     * <p>Returns:
     * <ul>
     *   <li>200 OK with calculated quote + draft application</li>
     * </ul>
     *
     * @param request    validated application payload
     * @param webRequest request metadata
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('APPLICATION:CREATE')")
    public ResponseEntity<CustomResponse<ApplicationResponse.Quote>> calculateAndDraft(
            @Valid @RequestBody ApplicationRequest.Create request,
            WebRequest webRequest
    ) {
        log.info(
                "POST /api/v1/applications/calculate — vehicleNo={}, state={}, taxType={}",
                request.getVehicleNumber(),
                request.getStateName(),
                request.getTaxType()
        );

        return applicationService.calculateAndDraft(request, webRequest);
    }

    @PostMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public String status() {
        return "Hello";
    }

    // =========================================================================
    // GET BY ID
    // =========================================================================

    /**
     * GET /api/v1/applications/{id}
     *
     * <p>Fetch complete application details by UUID.
     *
     * <p>Rules:
     * <ul>
     *   <li>Soft-deleted records return 404</li>
     *   <li>Returns full tax breakdown + payment snapshot</li>
     *   <li>Returns linked TaxRateConfig reference info</li>
     * </ul>
     *
     * <p>Returns:
     * <ul>
     *   <li>200 OK with Detail response</li>
     * </ul>
     *
     * @param id UUID of the application
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('APPLICATION:READ_ALL') or hasAuthority('APPLICATION:READ_OWN')")
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> getById(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("GET /api/v1/applications/{}", id);

        return applicationService.getById(id, webRequest);
    }

    // =========================================================================
    // SUBMIT APPLICATION
    // =========================================================================

    /**
     * PATCH /api/v1/applications/{id}/submit
     *
     * <p>Submit a DRAFT application for further processing/payment.
     *
     * <p>Rules:
     * <ul>
     *   <li>Only DRAFT applications can be submitted</li>
     *   <li>Status transition: DRAFT -> SUBMITTED</li>
     *   <li>submittedAt timestamp is automatically recorded</li>
     *   <li>Already submitted applications return 400</li>
     * </ul>
     *
     * <p>Returns:
     * <ul>
     *   <li>200 OK with updated application detail</li>
     * </ul>
     *
     * @param id UUID of the draft application
     */
    @PatchMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('APPLICATION:UPDATE_OWN') or hasAuthority('APPLICATION:UPDATE_ANY')")
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> submit(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/applications/{}/submit", id);

        return applicationService.submit(id, webRequest);
    }

    // =========================================================================
    // CANCEL APPLICATION
    // =========================================================================

    /**
     * PATCH /api/v1/applications/{id}/cancel
     *
     * <p>Cancel an application before payment completion.
     *
     * <p>Rules:
     * <ul>
     *   <li>PAID applications cannot be cancelled</li>
     *   <li>Status changes to CANCELLED</li>
     *   <li>Already-cancelled requests are idempotent</li>
     * </ul>
     *
     * <p>Returns:
     * <ul>
     *   <li>200 OK with null response body</li>
     * </ul>
     *
     * @param id UUID of the application
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('APPLICATION:CANCEL_OWN') or hasAuthority('APPLICATION:CANCEL_ANY')")
    public ResponseEntity<CustomResponse<Void>> cancel(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/applications/{}/cancel", id);

        return applicationService.cancel(id, webRequest);
    }

    // =========================================================================
    // MARK AS READ
    // =========================================================================

    /**
     * PATCH /api/v1/applications/{id}/read
     *
     * <p>Mark an application as read by admin or agent.
     *
     * <p>Rules:
     * <ul>
     *   <li>Sets isRead = true</li>
     *   <li>Does not change business status</li>
     *   <li>Idempotent operation</li>
     * </ul>
     *
     * <p>Used for:
     * <ul>
     *   <li>Dashboard unread badge clearing</li>
     *   <li>Operational acknowledgement tracking</li>
     * </ul>
     *
     * @param id UUID of the application
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('APPLICATION:READ_ALL')")
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> markAsRead(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/applications/{}/read", id);

        return applicationService.markAsRead(id, webRequest);
    }

    // =========================================================================
    // MARK AS COMPLETE
    // =========================================================================

    /**
     * PATCH /api/v1/applications/{id}/complete
     *
     * <p>Mark a PAID application as fully completed.
     *
     * <p>Rules:
     * <ul>
     *   <li>Only PAID applications can be completed</li>
     *   <li>Represents certificate delivery/admin closure</li>
     *   <li>Automatically marks application as read</li>
     * </ul>
     *
     * <p>Returns:
     * <ul>
     *   <li>200 OK with updated application detail</li>
     * </ul>
     *
     * @param id UUID of the paid application
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('APPLICATION:UPDATE_ANY')")
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> markAsComplete(
            @PathVariable UUID id,
            WebRequest webRequest
    ) {
        log.info("PATCH /api/v1/applications/{}/complete", id);

        return applicationService.markAsComplete(id, webRequest);
    }

    // =========================================================================
    // SEARCH APPLICATIONS
    // =========================================================================

    /**
     * GET /api/v1/applications/search
     *
     * <p>Paginated and filterable search across applications.
     *
     * <p>Supported filters:
     * <ul>
     *   <li>stateName  — partial match</li>
     *   <li>vehicleNo  — partial match</li>
     *   <li>status     — exact AppStatusEnum match</li>
     *   <li>isRead     — true / false</li>
     *   <li>fromDate   — startDate lower bound</li>
     *   <li>toDate     — startDate upper bound</li>
     * </ul>
     *
     * <p>Pagination:
     * <ul>
     *   <li>page — default 0</li>
     *   <li>size — default 20, max 100</li>
     * </ul>
     *
     * <p>Sorting:
     * <ul>
     *   <li>createdAt</li>
     *   <li>submittedAt</li>
     *   <li>status</li>
     *   <li>totalAmount</li>
     *   <li>startDate</li>
     * </ul>
     *
     * @param request    search filters + pagination
     * @param webRequest request metadata
     */
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('APPLICATION:READ_ALL') or hasAuthority('APPLICATION:READ_OWN')")
    public ResponseEntity<CustomResponse<PagedResponse<ApplicationResponse.Summary>>> search(
            @Valid @RequestParam ApplicationRequest.Search request,
            WebRequest webRequest
    ) {
        log.info(
                "GET /api/v1/applications/search — page={}, size={}, status={}, isRead={}",
                request.getPage(),
                request.getSize(),
                request.getStatus(),
                request.getIsRead()
        );

        return applicationService.search(request, webRequest);
    }

    // =========================================================================
    // COUNT UNREAD
    // =========================================================================

    /**
     * GET /api/v1/applications/unread/count
     *
     * <p>Get unread application count filtered by status.
     *
     * <p>Used for:
     * <ul>
     *   <li>Admin dashboard counters</li>
     *   <li>Notification badge indicators</li>
     * </ul>
     *
     * <p>Example:
     * <pre>
     * /api/v1/applications/unread/count?status=SUBMITTED
     * </pre>
     *
     * @param status application status filter
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasAuthority('APPLICATION:READ_ALL')")
    public ResponseEntity<CustomResponse<Long>> countUnread(
            @RequestParam AppStatusEnum status,
            WebRequest webRequest
    ) {
        log.info("GET /api/v1/applications/unread/count — status={}", status);

        return applicationService.countUnread(status, webRequest);
    }

    // =========================================================================
    // COUNT BY STATUS
    // =========================================================================

    /**
     * GET /api/v1/applications/status/count
     *
     * <p>Get total application count by status.
     *
     * <p>Used for:
     * <ul>
     *   <li>Analytics dashboard</li>
     *   <li>Status summary cards</li>
     *   <li>Operational metrics</li>
     * </ul>
     *
     * @param status application status enum
     */
    @GetMapping("/status/count")
    @PreAuthorize("hasAuthority('APPLICATION:READ_ALL') or hasAuthority('REPORT:VIEW_ALL')")
    public ResponseEntity<CustomResponse<Long>> countApplicationStatus(
            @RequestParam AppStatusEnum status,
            WebRequest webRequest
    ) {
        log.info("GET /api/v1/applications/status/count — status={}", status);

        return applicationService.countApplicationStatus(status, webRequest);
    }

    // =========================================================================
    // REFUND APPLICATION
    // =========================================================================

    /**
     * PATCH /api/v1/applications/{id}/refund
     *
     * <p>Refund a previously PAID application.
     *
     * <p>Rules:
     * <ul>
     *   <li>Only PAID applications are refundable</li>
     *   <li>Refund reason is mandatory</li>
     *   <li>Refund metadata is persisted for audit tracking</li>
     *   <li>Status transitions to REFUNDED (if supported)</li>
     * </ul>
     *
     * <p>Security:
     * <ul>
     *   <li>Restricted to ADMIN/SUPER_ADMIN</li>
     * </ul>
     *
     * @param id     UUID of the paid application
     * @param refund refund payload
     */
    @PatchMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('PAYMENT:REFUND')")
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> refundApplication(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ApplicationRequest.Refund refund,
            WebRequest webRequest
    ) {
        log.warn("PATCH /api/v1/applications/{}/refund", id);

        return applicationService.refundApplication(id, refund, webRequest);
    }

}
