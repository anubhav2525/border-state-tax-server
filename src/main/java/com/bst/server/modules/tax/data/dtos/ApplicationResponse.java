package com.bst.server.modules.tax.data.dtos;

import com.bst.server.modules.tax.data.entities.Application;
import com.bst.server.modules.tax.data.enums.*;
import lombok.*;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ApplicationResponse {
    private final ModelMapper modelMapper;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Quote {
        private UUID applicationId;         // DRAFT ID — use this to initiate payment

        // Submitted input (echoed back for confirmation screen)
        private String stateName;
        private String vehicleSeating;
        private String vehicleNo;
        private String chassisNumber;
        private String phoneNo;
        private TaxModeEnum taxType;
        private PaymentModeEnum paymentMode;

        // Computed date range
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer numberOfDays;

        // Computed amounts — these are LOCKED into the application record
        private BigDecimal baseTaxAmount;
        private BigDecimal commissionAmount;
        private BigDecimal totalAmount;     // = baseTaxAmount + commissionAmount

        // Rate config reference (for transparency / audit)
        private UUID taxRateConfigId;

        private AppStatusEnum status;       // Always DRAFT at this stage
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Detail {
        private UUID id;
        private String stateName;
        private String vehicleSeating;
        private String vehicleNumber;
        private String phoneNumber;
        private String chassisNumber;
        private TaxModeEnum taxType;
        private PaymentModeEnum paymentMode;

        private LocalDate startDate;
        private LocalDate endDate;
        private Integer numberOfDays;

        private BigDecimal baseTaxAmount;
        private BigDecimal commissionAmount;
        private BigDecimal totalAmount;

        private AppStatusEnum status;
        private Boolean isRead;

        private UUID taxRateConfigId;
        private UUID submittedBy;

        private LocalDateTime submittedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private UUID id;
        private String vehicleNumber;
        private String stateName;
        private String vehicleSeating;
        private String chassisNumber;
        private TaxModeEnum taxType;
        private PaymentModeEnum paymentMode;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal totalAmount;
        private AppStatusEnum status;
        private Boolean isRead;             // admin acknowledgment flag
        private LocalDateTime submittedAt;
        private LocalDateTime createdAt;
    }

    // mappers to map application
    public ApplicationResponse.Detail toDetail(Application application) {
        return modelMapper.map(application, ApplicationResponse.Detail.class);
    }

    public ApplicationResponse.Summary toSummary(Application application) {
        return modelMapper.map(application, ApplicationResponse.Summary.class);
    }

    public ApplicationResponse.Quote toQuote(Application application) {
        return modelMapper.map(application, ApplicationResponse.Quote.class);
    }
}
