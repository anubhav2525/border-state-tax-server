package com.bst.server.modules.tax.services;

import com.bst.server.common.utils.CustomResponse;
import com.bst.server.common.utils.PagedResponse;
import com.bst.server.modules.tax.data.dtos.ApplicationRequest;
import com.bst.server.modules.tax.data.dtos.ApplicationResponse;
import com.bst.server.modules.tax.data.enums.AppStatusEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    /**
     * Calculate tax amount based on form input and save the application as DRAFT.
     * <p>
     * Rules:
     * - An active TaxRateConfig must exist for the given stateName + vehicleSeating + taxType
     * - The selected paymentMode must have a non-zero rate in the matched config
     * - End date and number of days are auto-computed based on the paymentMode:
     * DAILY      -> same day (1 day)
     * WEEKLY     -> startDate + 6 days (7 days)
     * MONTHLY    -> startDate + 1 month - 1 day
     * QUARTERLY  -> startDate + 3 months - 1 day
     * YEARLY     -> startDate + 1 year - 1 day
     * - Amounts (baseTaxAmount, commissionAmount, totalAmount) are snapshotted at this
     * point and must never be recalculated from live rates after submission
     * - Application is persisted with status DRAFT
     * <p>
     * Throws:
     * - ResourceNotExistsException    (no active config found for the combination)
     * - ResourceValidationException   (selected paymentMode has zero rate)
     *
     * @param request    - ApplicationRequest.Create (form input from user)*
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<ApplicationResponse.Quote>> calculateAndDraft(
            ApplicationRequest.Create request, WebRequest webRequest);

    /**
     * Fetch a single application by its UUID.
     * <p>
     * Rules:
     * - Only non-deleted records are returned
     * - Eagerly loads TaxRateConfig to avoid N+1 queries on detail view
     * <p>
     * Throws:
     * - ResourceNotExistsException  (id not found or record is deleted)
     *
     * @param id         - UUID of the application
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<ApplicationResponse.Detail>> getById(
            UUID id, WebRequest webRequest);

    /**
     * Submit a DRAFT application - indicates user has reviewed the quote and confirmed.
     * <p>
     * Rules:
     * - Only applications in DRAFT status can be submitted
     * - Transitions status: DRAFT -> SUBMITTED
     * - Sets submittedAt timestamp on the record
     * - This step is optional - payment can also be initiated directly from DRAFT
     * <p>
     * Throws:
     * - ResourceNotExistsException    (id not found)
     * - ResourceValidationException   (status is not DRAFT)
     *
     * @param id         - UUID of the DRAFT application
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<ApplicationResponse.Detail>> submit(
            UUID id, WebRequest webRequest);

    /**
     * Cancel an application that has not yet been paid.
     * <p>
     * Rules:
     * - Transitions status to CANCELLED
     * - PAID applications cannot be cancelled - use a refund flow instead
     * - Calling on an already-CANCELLED application is a no-op (idempotent)
     * <p>
     * Throws:
     * - ResourceNotExistsException   (id not found)
     * - ResourceOperationNotAllowed  (application is already PAID)
     *
     * @param id         - UUID of the application to cancel
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<Void>> cancel(
            UUID id, WebRequest webRequest);

    /**
     * Mark an application as read by the admin or agent.
     * <p>
     * Rules:
     * - Sets isRead = true on the application record
     * - Does not change the application status
     * - Calling on an already-read application is a no-op (idempotent)
     * - Used to clear unread badge on admin dashboard
     * <p>
     * Throws:
     * - ResourceNotExistsException  (id not found)
     *
     * @param id         - UUID of the application to acknowledge
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<ApplicationResponse.Detail>> markAsRead(
            UUID id, WebRequest webRequest);

    /**
     * Mark a PAID application as complete (admin finalisation).
     * <p>
     * Rules:
     * - Only applications in PAID status can be marked complete
     * - Sets isRead = true as a side effect (admin has reviewed it)
     * - Represents physical certificate handover or final admin sign-off
     * - If COMPLETED state is added to AppStatusEnum in future,
     * the status transition should happen here
     * <p>
     * Throws:
     * - ResourceNotExistsException    (id not found)
     * - ResourceValidationException   (status is not PAID)
     *
     * @param id         - UUID of the PAID application
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<ApplicationResponse.Detail>> markAsComplete(
            UUID id, WebRequest webRequest);

    /**
     * Paginated, filtered, and sortable search across all applications.
     * <p>
     * Supported filters:
     * - stateName  : partial match (case-insensitive LIKE)
     * - vehicleNo  : partial match (case-insensitive LIKE)
     * - status     : exact match (AppStatusEnum value)
     * - isRead     : true / false / null (all)
     * - fromDate   : startDate >= fromDate
     * - toDate     : startDate <= toDate
     * <p>
     * Supported sort fields:
     * - createdAt, submittedAt, status, totalAmount, startDate
     * <p>
     * Pagination:
     * - page (0-based), size (default 20, max 100)
     *
     * @param request    - ApplicationRequest.Search (filters + pagination + sort)
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<PagedResponse<ApplicationResponse.Summary>>> search(
            ApplicationRequest.Search request, WebRequest webRequest);

    /**
     * Get count of unread applications filtered by status.
     * <p>
     * Rules:
     * - Returns count of applications where isRead = false and status matches
     * - Used by admin dashboard to display unread badge counts per status
     * e.g. how many SUBMITTED or PAID applications are pending review
     * <p>
     * Throws:
     * - None - returns 0 if no matching records exist
     *
     * @param status     - AppStatusEnum to filter by (e.g. SUBMITTED)
     * @param webRequest - Api request header
     */
    ResponseEntity<CustomResponse<Long>> countUnread(
            AppStatusEnum status, WebRequest webRequest);

    ResponseEntity<CustomResponse<Long>> countApplicationStatus(
            AppStatusEnum statusEnum, WebRequest request
    );

    ResponseEntity<CustomResponse<ApplicationResponse.Detail>> refundApplication(
            UUID applicationId,
            ApplicationRequest.Refund refund,
            WebRequest webRequest
    );
}