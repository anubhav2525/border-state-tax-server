package com.bst.server.modules.tax.services.impl;

import com.bst.server.common.exceptions.sub.ResourceNotExistsException;
import com.bst.server.common.exceptions.sub.ResourceOperationNotAllowed;
import com.bst.server.common.exceptions.sub.ResourceValidationException;
import com.bst.server.common.utils.*;
import com.bst.server.modules.tax.data.dtos.ApplicationRequest;
import com.bst.server.modules.tax.data.dtos.ApplicationResponse;
import com.bst.server.modules.tax.data.entities.Application;
import com.bst.server.modules.tax.data.entities.TaxRateConfig;
import com.bst.server.modules.tax.data.enums.AppStatusEnum;
import com.bst.server.modules.tax.data.enums.PaymentModeEnum;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import com.bst.server.modules.tax.repository.ApplicationRepository;
import com.bst.server.modules.tax.services.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.WebRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final ApplicationResponse applicationResponse;
    private final BuildPageable buildPageable;
    private final CreateResponseEntity createResponseEntity;
    private final ApplicationTaxRateConfigService applicationTaxRateConfigService;
    private final StringOperation stringOperation;

    private record AmountResult(
//            LocalDate startDate,
            LocalDate endDate,
            int numberOfDays,
            BigDecimal baseTaxAmount,
            BigDecimal commissionAmount,
            BigDecimal totalAmount
    ) {
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "stateName",
            "taxType",
            "paymentMode",
            "status",
            "handledBy",
            "createdAt",
            "updatedAt",
            "enabled"
    );

    private static final String DEFAULT_SORT_FIELD = "createdAt";

    private static final String RESOURCE = "Application";

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<ApplicationResponse.Quote>> calculateAndDraft(
            ApplicationRequest.Create request, WebRequest webRequest) {

        // extract values
        String stateName = stringOperation.trimOrNull(request.getStateName());
        String vehicleSeating = stringOperation.trimOrNull(request.getVehicleSeating());
        TaxModeEnum taxType = request.getTaxType();

        // 1a. Fetch active tax rate config matching state + seating + taxType
        TaxRateConfig taxRateConfig = applicationTaxRateConfigService.
                findByStateNameIgnoreCaseAndVehicleSeatingIgnoreCaseAndTaxType(
                        stateName, vehicleSeating, taxType);

        // 1b. Guard: selected payment mode must have a non-zero rate in config
        validateModeIsConfigured(taxRateConfig, request.getPaymentMode());

        // 1c. Compute date range + amounts
        // Request validate karo (endDate check for DAILY)
        request.validate();

        // computeAmounts mein endDate pass karo
        AmountResult amounts = computeAmounts(
                taxRateConfig,
                request.getPaymentMode(),
                request.getStartDate(),
                request.getEndDate()   // ← DAILY ke liye user ka endDate, baaki ke liye null
        );

        // 1d. Prevent duplicate DRAFT for same vehicle (optional business rule)
        if (applicationRepository.existsByVehicleNumberAndPaymentModeAndStatus(
                request.getVehicleNumber(), request.getPaymentMode(), AppStatusEnum.DRAFT)) {
            log.warn("Duplicate DRAFT detected for vehicle={} mode={}", request.getVehicleNumber(), request.getPaymentMode());
            // Warn only — do NOT block, user may want a new quote
        }

        Application application = Application.builder()
                .stateName(stateName)
                .vehicleSeating(vehicleSeating)
                .vehicleNumber(request.getVehicleNumber())
                .chassisNumber(request.getTaxType() == TaxModeEnum.BORDER
                        ? null
                        : stringOperation.trimOrNull(request.getChassisNumber()))
                .phoneNumber(request.getPhoneNumber())
                .taxType(request.getTaxType())
                .taxRateConfig(taxRateConfig)
                .paymentMode(request.getPaymentMode())
                .startDate(request.getStartDate())
                .endDate(amounts.endDate)
                .numberOfDays(amounts.numberOfDays())
                .baseTaxAmount(amounts.baseTaxAmount)
                .commissionAmount(amounts.commissionAmount)
                .totalAmount(amounts.totalAmount)
                .status(AppStatusEnum.DRAFT)
                .isRead(false)
                .build();

        application = applicationRepository.save(application);
        log.info("DRAFT created: id={} vehicle={} total={}",
                application.getId(), application.getVehicleNumber(), application.getTotalAmount());

        return createResponseEntity.buildResponse(
                RESOURCE + " draft created successfully",
                applicationResponse.toQuote(application),
                HttpStatus.CREATED,
                webRequest
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> getById(
            UUID id, WebRequest webRequest) {

        Application app = applicationRepository.findByIdWithConfig(id)
                .orElseThrow(() -> new ResourceNotExistsException("Application not found: " + id));

        return createResponseEntity.buildResponse(
                RESOURCE + " fetched successfully",
                applicationResponse.toDetail(app),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> submit(
            UUID id, WebRequest webRequest) {
        return null;
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<Void>> cancel(
            UUID id, WebRequest webRequest) {
        Application app = findOrThrow(id);

        // if application status is not draft then it will be a wrong application
        //in this scenario the application can not be cancelled
//        if (!app.getStatus().equals(AppStatusEnum.DRAFT))
//            return createResponseEntity.buildResponse(
//                    "Wrong application",
//                    null,
//                    HttpStatus.BAD_REQUEST,
//                    webRequest);

        switch (app.getStatus()) {
            case CANCELLED -> throw new ResourceOperationNotAllowed(RESOURCE + " is already cancelled.");

            case SUBMITTED -> throw new ResourceOperationNotAllowed(
                    "Submitted " + RESOURCE + " cannot be cancelled. Contact admin.");

            case PAYMENT_PENDING -> throw new ResourceOperationNotAllowed(
                    "Payment is in progress. Wait for it to complete or expire before cancelling.");

            case COMPLETED -> throw new ResourceOperationNotAllowed("Completed application can't be cancelled");

            case REFUND -> throw new ResourceOperationNotAllowed(
                    RESOURCE + " is already in refund process. Cannot cancel.");

            case DRAFT -> {
                app.setStatus(AppStatusEnum.CANCELLED);
                applicationRepository.save(app);
                log.info("Application cancelled: id={}", id);

                return createResponseEntity.buildResponse(
                        RESOURCE + " cancelled successfully",
                        null,
                        HttpStatus.OK,
                        webRequest);
            }

            default -> throw new IllegalStateException(
                    "Unhandled application status: " + app.getStatus());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> markAsRead(
            UUID id, WebRequest webRequest) {

        Application app = findOrThrow(id);

        if (Boolean.TRUE.equals(app.getIsRead()))
            throw new ResourceOperationNotAllowed(
                    RESOURCE + " is already marked as read.");

        app.setIsRead(true);

        // TODO: set which user is handled this case
        // app.setHandledBy();

        applicationRepository.save(app);

        log.info("Application marked as read: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " marked as read",
                null,
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> markAsComplete(
            UUID id, WebRequest webRequest) {
        Application app = findOrThrow(id);

        if (app.getStatus().equals(AppStatusEnum.COMPLETED)) {
            throw new ResourceOperationNotAllowed(
                    RESOURCE + " is already completed.");
        }

        if (!app.getStatus().equals(AppStatusEnum.SUBMITTED)) {
            throw new ResourceOperationNotAllowed(
                    RESOURCE + " cannot be marked complete. Current status: " + app.getStatus()
                            + ". Only PAID applications can be marked as complete.");
        }
        app.setStatus(AppStatusEnum.COMPLETED);
        app.setIsRead(true);

        // TODO: set who is handle the case
//        app.setHandledBy();

        applicationRepository.save(app);

        log.info("Application marked as read: id={}", id);

        return createResponseEntity.buildResponse(
                RESOURCE + " marked as complete",
                applicationResponse.toDetail(app),
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<PagedResponse<ApplicationResponse.Summary>>> search(
            ApplicationRequest.Search request, WebRequest webRequest) {
        log.info("Searching applications: page={}, size={}, sortBy={}, sortDir={}",
                request.getPage(), request.getSize(), request.getSortBy(), request.getSortDir());

        Pageable pageable = buildPageable.build(
                request.getPage(), request.getSize(),
                request.getSortBy(), request.getSortDir(),
                ALLOWED_SORT_FIELDS, DEFAULT_SORT_FIELD
        );

        Specification<Application> specification = buildSpec(request);

        Page<Application> page = applicationRepository.findAll(specification, pageable);

        List<ApplicationResponse.Summary> content = page.getContent()
                .stream()
                .map(applicationResponse::toSummary)
                .toList();

        PagedResponse<ApplicationResponse.Summary> response =
                PagedResponse.<ApplicationResponse.Summary>builder()
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
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<Long>> countUnread(
            AppStatusEnum status, WebRequest webRequest) {
        Long count = applicationRepository.countByIsReadFalseAndStatus(status);

        log.info("Unread count for status={}: {}", status, count);

        return createResponseEntity.buildResponse(
                "Unread " + RESOURCE + " count fetched",
                count,
                HttpStatus.OK,
                webRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<CustomResponse<Long>> countApplicationStatus(
            AppStatusEnum statusEnum, WebRequest webRequest) {
        long count = applicationRepository.countByStatus(statusEnum);

        log.info("{} count for status={}: {}", RESOURCE, statusEnum, count);

        return createResponseEntity.buildResponse(
                RESOURCE + " count for status " + statusEnum + " fetched",
                count,
                HttpStatus.OK,
                webRequest);
    }

    @Override
    public ResponseEntity<CustomResponse<ApplicationResponse.Detail>> refundApplication(
            UUID applicationId, ApplicationRequest.Refund refund, WebRequest request) {
        Application app = findOrThrow(applicationId);

        if (app.getStatus().equals(AppStatusEnum.REFUND)) {
            throw new ResourceOperationNotAllowed(
                    RESOURCE + " is already in refund process.");
        }

        if (!app.getStatus().equals(AppStatusEnum.SUBMITTED)) {
            throw new ResourceOperationNotAllowed(
                    RESOURCE + " cannot be refunded. Current status: " + app.getStatus()
                            + ". Only PAID applications are eligible for refund.");
        }

        if (refund.getReason() == null || refund.getReason().isBlank()) {
            throw new ResourceValidationException("Refund reason is required.");
        }

        app.setStatus(AppStatusEnum.REFUND);
        app.setRefundReason(stringOperation.trimOrNull(refund.getReason().trim()));
        // TODO: trigger PaymentService.initiateRefund(applicationId) for Razorpay refund
        applicationRepository.save(app);

        log.info("Refund initiated: id={} reason={}", applicationId, refund.getReason());

        return createResponseEntity.buildResponse(
                RESOURCE + " refund initiated successfully",
                applicationResponse.toDetail(app),
                HttpStatus.OK,
                request);
    }

// ═════════════════════════════════════════════════════════════════════════
// PRIVATE — Amount Calculation
// ═════════════════════════════════════════════════════════════════════════

    /**
     * Computes end date, number of days, base tax, commission, and total
     * based on the payment mode selected by the user.
     * <p>
     * Rate logic:
     * DAILY     → 1 day,           rate = dailyRate / dailyCommission
     * WEEKLY    → 7 days,          rate = weeklyRate / weeklyCommission
     * MONTHLY   → 1 calendar month rate = monthlyRate / monthlyCommission
     * QUARTERLY → 3 calendar months
     * YEARLY    → 1 calendar year
     * <p>
     * Important: rates in tax_rate_configs represent the FLAT amount for
     * that entire period — not a per-day rate. So totalAmount is simply
     * rate + commission regardless of exact day count.
     */
    private AmountResult computeAmounts(TaxRateConfig cfg, PaymentModeEnum mode, LocalDate start, LocalDate requestedEndDate) {
        return switch (mode) {
            case DAILY -> {

                // endDate user se aata hai, days count karo
                int days = daysBetween(start, requestedEndDate);

                BigDecimal baseTax = cfg.getDailyRate()
                        .multiply(BigDecimal.valueOf(days));

                BigDecimal rawCommission = cfg.getDailyCommission()
                        .multiply(BigDecimal.valueOf(days));

                // Cap: commission kabhi weeklyCommission se zyada nahi hoga
                // Agar weeklyCommission = 0 (not configured), toh raw use karo
                BigDecimal weeklyCommission = cfg.getWeeklyCommission();
                BigDecimal commission = (weeklyCommission.compareTo(BigDecimal.ZERO) > 0
                        && rawCommission.compareTo(weeklyCommission) > 0)
                        ? weeklyCommission      // cap applied
                        : rawCommission;        // no cap needed

                yield build(
                        start,
                        requestedEndDate,                          // end = same day
                        days,
                        baseTax,
                        commission
                );
            }
            case WEEKLY -> {
                // endDate auto-compute, user sirf startDate deta hai
                LocalDate end = start.plusDays(6); // 7 days: Mon → Sun
                yield build(start, end, 7,
                        cfg.getWeeklyRate(), cfg.getWeeklyCommission());
            }
            case MONTHLY -> {
                LocalDate end = start.plusMonths(1).minusDays(1);
                yield build(start, end, daysBetween(start, end),
                        cfg.getMonthlyRate(), cfg.getMonthlyCommission());
            }
            case QUARTERLY -> {
                LocalDate end = start.plusMonths(3).minusDays(1);
                yield build(start, end, daysBetween(start, end),
                        cfg.getQuarterlyRate(), cfg.getQuarterlyCommission());
            }
            case YEARLY -> {
                LocalDate end = start.plusYears(1).minusDays(1);
                yield build(start, end, daysBetween(start, end),
                        cfg.getYearlyRate(), cfg.getYearlyCommission());
            }
        };
    }

    private AmountResult build(LocalDate start, LocalDate end, int days,
                               BigDecimal rate, BigDecimal commission) {
        return new AmountResult(end, days, rate, commission, rate.add(commission));
    }

    private int daysBetween(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1; // inclusive
    }

    /**
     * Ensures the chosen payment mode has a non-zero rate in the config.
     * Prevents user from paying 0 due to unconfigured modes.
     */
    private void validateModeIsConfigured(TaxRateConfig cfg, PaymentModeEnum mode) {
        BigDecimal rate = switch (mode) {
            case DAILY -> cfg.getDailyRate();
            case WEEKLY -> cfg.getWeeklyRate();
            case MONTHLY -> cfg.getMonthlyRate();
            case QUARTERLY -> cfg.getQuarterlyRate();
            case YEARLY -> cfg.getYearlyRate();
        };
        if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            throw new ResourceValidationException(
                    mode + " payment mode has no rate configured for this state/vehicle combination.");
        }
    }

// ═════════════════════════════════════════════════════════════════════════
// PRIVATE — Mappers
// ═════════════════════════════════════════════════════════════════════════

    private Application findOrThrow(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotExistsException("Application not found: " + id));
    }

// ═════════════════════════════════════════════════════════════════════════
// PRIVATE — JPA Specification (dynamic filter for search)
// ═════════════════════════════════════════════════════════════════════════

    private Specification<Application> buildSpec(ApplicationRequest.Search req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getStateName() != null && !req.getStateName().isBlank())
                predicates.add(cb.like(cb.lower(root.get("stateName")),
                        "%" + req.getStateName().toLowerCase() + "%"));

            if (req.getVehicleNo() != null && !req.getVehicleNo().isBlank())
                predicates.add(cb.like(cb.lower(root.get("vehicleNumber")),
                        "%" + req.getVehicleNo().toLowerCase() + "%"));

            if (req.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), req.getStatus()));

            if (req.getIsRead() != null)
                predicates.add(cb.equal(root.get("isRead"), req.getIsRead()));

            if (req.getFromDate() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), req.getFromDate()));

            if (req.getToDate() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), req.getToDate()));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
