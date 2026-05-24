package com.bst.server.modules.tax.data.dtos;

import com.bst.server.modules.tax.data.entities.TaxRateConfig;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import lombok.*;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaxRateConfigResponse {
    private final ModelMapper modelMapper;

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class Summary {
        private UUID id;
        private String stateName;
        private String vehicleSeating;
        private TaxModeEnum taxType;

        // Show one representative rate per mode for quick scanning in tables
        // These are the base rates (without commission)
        private BigDecimal dailyRate;
        private BigDecimal weeklyRate;
        private BigDecimal monthlyRate;
        private BigDecimal quarterlyRate;
        private BigDecimal yearlyRate;

        // Availability flags — UI uses these to show/hide mode columns in list
        private boolean dailyAvailable;
        private boolean weeklyAvailable;
        private boolean monthlyAvailable;
        private boolean quarterlyAvailable;
        private boolean yearlyAvailable;

        private Boolean isActive;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    @Builder
    public static class Detail {

        private UUID id;
        private String stateName;
        private String vehicleSeating;
        private TaxModeEnum taxType;

        // ── Daily ──────────────────────────────────────────────────────────────────
        private BigDecimal dailyRate;
        private BigDecimal dailyCommission;
        private BigDecimal dailyTotal;          // computed: dailyRate + dailyCommission
        private boolean dailyAvailable;

        // ── Weekly ─────────────────────────────────────────────────────────────────
        private BigDecimal weeklyRate;
        private BigDecimal weeklyCommission;
        private BigDecimal weeklyTotal;
        private boolean weeklyAvailable;

        // ── Monthly ────────────────────────────────────────────────────────────────
        private BigDecimal monthlyRate;
        private BigDecimal monthlyCommission;
        private BigDecimal monthlyTotal;
        private boolean monthlyAvailable;

        // ── Quarterly ──────────────────────────────────────────────────────────────
        private BigDecimal quarterlyRate;
        private BigDecimal quarterlyCommission;
        private BigDecimal quarterlyTotal;
        private boolean quarterlyAvailable;

        // ── Yearly ─────────────────────────────────────────────────────────────────
        private BigDecimal yearlyRate;
        private BigDecimal yearlyCommission;
        private BigDecimal yearlyTotal;
        private boolean yearlyAvailable;

        // ── Status & Audit ─────────────────────────────────────────────────────────
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    public Summary toSummary(TaxRateConfig taxRateConfig) {
        return modelMapper.map(taxRateConfig, TaxRateConfigResponse.Summary.class);
    }

    public Detail toDetail(TaxRateConfig taxRateConfig) {
        return modelMapper.map(taxRateConfig, Detail.class);
    }


}