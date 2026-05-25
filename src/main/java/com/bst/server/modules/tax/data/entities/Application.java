package com.bst.server.modules.tax.data.entities;

import com.bst.server.modules.tax.data.enums.AppStatusEnum;
import com.bst.server.modules.tax.data.enums.PaymentModeEnum;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "applications",
        indexes = {
                @Index(name = "idx_app_status", columnList = "status"),
                @Index(name = "idx_app_vehicle_no", columnList = "vehicle_no"),
                @Index(name = "idx_app_dates", columnList = "start_date, end_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ── Rate matrix selection ──────────────────────
    @Column(name = "state_name", nullable = false, length = 30)
    private String stateName;

    @Column(name = "vehicle_seating", nullable = false, length = 30)
    private String vehicleSeating;

    @Column(name = "vehicle_no", nullable = false, length = 10)
    private String vehicleNumber;

    @Column(name = "phone_no", nullable = false, length = 10)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tax_type", nullable = false, columnDefinition = "tax_mode_enum")
    private TaxModeEnum taxType;

    /**
     * Rate config is snapshotted (locked) at submission time.
     * Amounts must never be recalculated from live rates after submission.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tax_rate_config_id", nullable = false)
    private TaxRateConfig taxRateConfig;

    // ── Payment period ─────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false, columnDefinition = "payment_mode_enum")
    private PaymentModeEnum paymentMode;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "number_of_days", nullable = false)
    private Integer numberOfDays;

    // ── Calculated amounts (snapshot at submission) ─
    @Column(name = "base_tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseTaxAmount;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    /**
     * Must always equal baseTaxAmount + commissionAmount
     */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // ── Lifecycle ──────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "app_status_enum")
    @Builder.Default
    private AppStatusEnum status = AppStatusEnum.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
