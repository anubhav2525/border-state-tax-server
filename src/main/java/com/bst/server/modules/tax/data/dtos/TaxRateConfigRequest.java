package com.bst.server.modules.tax.data.dtos;

import com.bst.server.common.exceptions.sub.ResourceValidationException;
import com.bst.server.modules.tax.data.entities.TaxRateConfig;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.*;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaxRateConfigRequest {
    private final ModelMapper modelMapper;

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Data
    public static class Create {
        @NotBlank(message = "State name is required")
        @Size(max = 30, message = "State name must not exceed 30 characters")
        private String stateName;

        @NotBlank(message = "Vehicle seating is required")
        @Size(max = 30, message = "Vehicle seating must not exceed 30 characters")
        @Pattern(
                regexp = "^\\d+\\+1$",
                message = "Vehicle seating must be in format like 4+1, 7+1, 12+1"
        )
        private String vehicleSeating;

        @NotNull(message = "Tax type is required")
        private TaxModeEnum taxType;

        // ── Daily ──────────────────────────────────────────────────────────────────
        @NotNull(message = "Daily rate is required")
        @DecimalMin(value = "0.00", message = "Daily rate cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Daily rate: max 8 digits and 2 decimal places")
        private BigDecimal dailyRate;

        @NotNull(message = "Daily commission is required")
        @DecimalMin(value = "0.00", message = "Daily commission cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Daily commission: max 8 digits and 2 decimal places")
        private BigDecimal dailyCommission;

        // ── Weekly ─────────────────────────────────────────────────────────────────
        @NotNull(message = "Weekly rate is required")
        @DecimalMin(value = "0.00", message = "Weekly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal weeklyRate;

        @NotNull(message = "Weekly commission is required")
        @DecimalMin(value = "0.00", message = "Weekly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal weeklyCommission;

        // ── Monthly ────────────────────────────────────────────────────────────────
        @NotNull(message = "Monthly rate is required")
        @DecimalMin(value = "0.00", message = "Monthly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal monthlyRate;

        @NotNull(message = "Monthly commission is required")
        @DecimalMin(value = "0.00", message = "Monthly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal monthlyCommission;

        // ── Quarterly ──────────────────────────────────────────────────────────────
        @NotNull(message = "Quarterly rate is required")
        @DecimalMin(value = "0.00", message = "Quarterly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal quarterlyRate;

        @NotNull(message = "Quarterly commission is required")
        @DecimalMin(value = "0.00", message = "Quarterly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal quarterlyCommission;

        // ── Yearly ─────────────────────────────────────────────────────────────────
        @NotNull(message = "Yearly rate is required")
        @DecimalMin(value = "0.00", message = "Yearly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal yearlyRate;

        @NotNull(message = "Yearly commission is required")
        @DecimalMin(value = "0.00", message = "Yearly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal yearlyCommission;

        /**
         * Cross-field validation — at least one mode must be non-zero.
         * Called manually in service before saving.
         */
        public void validate() {
            boolean allZero =
                    isZero(dailyRate) && isZero(dailyCommission) &&
                            isZero(weeklyRate) && isZero(weeklyCommission) &&
                            isZero(monthlyRate) && isZero(monthlyCommission) &&
                            isZero(quarterlyRate) && isZero(quarterlyCommission) &&
                            isZero(yearlyRate) && isZero(yearlyCommission);

            if (allZero) {
                throw new ResourceValidationException(
                        "At least one payment mode must have a non-zero rate or commission");
            }
        }

        private boolean isZero(BigDecimal value) {
            return value == null || value.compareTo(BigDecimal.ZERO) == 0;
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class Update {
        // ── Daily ──────────────────────────────────────────────────────────────────
        @DecimalMin(value = "0.00", message = "Daily rate cannot be negative")
        @Digits(integer = 8, fraction = 2, message = "Daily rate: max 8 digits and 2 decimal places")
        private BigDecimal dailyRate;

        @DecimalMin(value = "0.00", message = "Daily commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal dailyCommission;

        // ── Weekly ─────────────────────────────────────────────────────────────────
        @DecimalMin(value = "0.00", message = "Weekly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal weeklyRate;

        @DecimalMin(value = "0.00", message = "Weekly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal weeklyCommission;

        // ── Monthly ────────────────────────────────────────────────────────────────
        @DecimalMin(value = "0.00", message = "Monthly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal monthlyRate;

        @DecimalMin(value = "0.00", message = "Monthly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal monthlyCommission;

        // ── Quarterly ──────────────────────────────────────────────────────────────
        @DecimalMin(value = "0.00", message = "Quarterly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal quarterlyRate;

        @DecimalMin(value = "0.00", message = "Quarterly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal quarterlyCommission;

        // ── Yearly ─────────────────────────────────────────────────────────────────
        @DecimalMin(value = "0.00", message = "Yearly rate cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal yearlyRate;

        @DecimalMin(value = "0.00", message = "Yearly commission cannot be negative")
        @Digits(integer = 8, fraction = 2)
        private BigDecimal yearlyCommission;

        /**
         * Returns true if the request carries at least one field to update.
         * Prevents empty PATCH calls from hitting the DB.
         */
        public boolean hasAnyUpdate() {
            return dailyRate != null || dailyCommission != null ||
                    weeklyRate != null || weeklyCommission != null ||
                    monthlyRate != null || monthlyCommission != null ||
                    quarterlyRate != null || quarterlyCommission != null ||
                    yearlyRate != null || yearlyCommission != null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Search {
        private String stateName;

        private String vehicleSeating;

        @Enumerated(EnumType.STRING)
        private TaxModeEnum taxType;

        private Boolean enabled;

        @Min(value = 0, message = "Page index must be 0 or greater")
        @Builder.Default
        private Integer page = 0;

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size must not exceed 100")
        @Builder.Default
        private Integer size = 20;

        @Builder.Default
        private String sortBy = "createdAt";

        @Pattern(
                regexp = "^(asc|desc)$",
                message = "Sort direction must be 'asc' or 'desc'"
        )
        @Builder.Default
        private String sortDir = "desc";
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class BulkDelete {
        private List<UUID> ids;
    }

    public TaxRateConfig toTaxRateConfig(TaxRateConfigRequest.Create request) {
        return modelMapper.map(request, TaxRateConfig.class);
    }

    public TaxRateConfig toTaxRateConfig(TaxRateConfigRequest.Update request) {
        return modelMapper.map(request, TaxRateConfig.class);
    }
}
