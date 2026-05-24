package com.bst.server.modules.tax.data.entities;

import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tax_rate_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRateConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "state_name", nullable = false, length = 30)
    private String stateName;

    @Column(name = "vehicle_seating", nullable = false, length = 30)
    private String vehicleSeating;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tax_type", nullable = false, columnDefinition = "tax_mode_enum")
    private TaxModeEnum taxType;

    // ── Daily ──────────────────────────────────────
    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal dailyRate = BigDecimal.ZERO;

    @Column(name = "daily_commission", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal dailyCommission = BigDecimal.ZERO;

    // ── Weekly ─────────────────────────────────────
    @Column(name = "weekly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal weeklyRate = BigDecimal.ZERO;

    @Column(name = "weekly_commission", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal weeklyCommission = BigDecimal.ZERO;

    // ── Monthly ────────────────────────────────────
    @Column(name = "monthly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyRate = BigDecimal.ZERO;

    @Column(name = "monthly_commission", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyCommission = BigDecimal.ZERO;

    // ── Quarterly ──────────────────────────────────
    @Column(name = "quarterly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quarterlyRate = BigDecimal.ZERO;

    @Column(name = "quarterly_commission", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quarterlyCommission = BigDecimal.ZERO;

    // ── Yearly ─────────────────────────────────────
    @Column(name = "yearly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal yearlyRate = BigDecimal.ZERO;

    @Column(name = "yearly_commission", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal yearlyCommission = BigDecimal.ZERO;

    // ── Validity ───────────────────────────────────
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}