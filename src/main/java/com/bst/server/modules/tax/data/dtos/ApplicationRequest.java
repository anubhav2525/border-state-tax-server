package com.bst.server.modules.tax.data.dtos;

import com.bst.server.common.exceptions.sub.ResourceValidationException;
import com.bst.server.modules.tax.data.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class ApplicationRequest {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Create {

        @NotBlank(message = "State name is required")
        @Size(max = 30, message = "State name must not exceed 30 characters")
        private String stateName;

        @NotBlank(message = "Vehicle seating is required")
        @Size(max = 30)
        @Pattern(
                regexp = "^\\d+\\+1$",
                message = "Vehicle seating must be in format: 4+1, 7+1, 12+1"
        )
        private String vehicleSeating;

        @NotBlank(message = "Vehicle number is required")
        @Size(max = 20, message = "Vehicle number must not exceed 20 characters")
        private String vehicleNumber;

        @NotBlank(message = "Chassis number is required")
        @Size(max = 20, message = "Chassis number must not exceed 20 characters")
        private String chassisNumber;

        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^[0-9]{10,15}$",
                message = "Phone must be 10–15 digits (no spaces or dashes)"
        )
        private String phoneNumber;

        @NotNull(message = "Tax type is required")
        private TaxModeEnum taxType;

        @NotNull(message = "Payment mode is required")
        private PaymentModeEnum paymentMode;

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date cannot be in the past")
        private LocalDate startDate;
        /**
         * Only required for DAILY mode.
         * For WEEKLY/MONTHLY/QUARTERLY/YEARLY → endDate is auto-computed from startDate.
         * Must be >= startDate and not more than 6 days ahead (max 7 days for DAILY).
         */
        private LocalDate endDate;

        /**
         * Called in service before calculation.
         * Validates endDate presence and range for DAILY mode.
         */
        public void validate() {
            if (paymentMode == PaymentModeEnum.DAILY) {
                if (endDate == null) {
                    throw new ResourceValidationException(
                            "End date is required for DAILY payment mode.");
                }
                if (endDate.isBefore(startDate)) {
                    throw new ResourceValidationException(
                            "End date cannot be before start date.");
                }
                long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                if (days > 6) {
                    throw new ResourceValidationException(
                            "DAILY mode supports max 6 days. " +
                                    "For 7+ days please use WEEKLY mode. Selected days: " + days);
                }
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Search {

        private String stateName;
        private String vehicleNo;
        private AppStatusEnum status;
        private Boolean isRead;           // filter unread applications for admin
        private LocalDate fromDate;       // start_date >= fromDate
        private LocalDate toDate;         // start_date <= toDate

        @Min(value = 0, message = "Page index must be 0 or greater")
        @Builder.Default
        private Integer page = 0;

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        @Builder.Default
        private Integer size = 20;

        @Builder.Default
        private String sortBy = "createdAt";

        @Pattern(regexp = "^(asc|desc)$", message = "Sort direction must be 'asc' or 'desc'")
        @Builder.Default
        private String sortDir = "desc";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Refund {
        @NotBlank(message = "Reason is required")
        private String reason;
    }
}
