package com.bst.server.modules.tax.repository;

import com.bst.server.modules.tax.data.entities.TaxRateConfig;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRateConfigRepository
        extends JpaRepository<TaxRateConfig, UUID>,
        JpaSpecificationExecutor<TaxRateConfig> {

    // ─────────────────────────────────────────────────────────────────────────
    // BULK STATUS UPDATE — used if bulkToggleEnabled is added later
    // ─────────────────────────────────────────────────────────────────────────
    @Modifying
    @Query("""
                UPDATE TaxRateConfig t
                SET t.enabled = :enabled
                WHERE t.id IN :ids
                  AND t.deleted = false
            """)
    int bulkSetActive(@Param("ids") List<UUID> ids, @Param("enabled") boolean enabled);

    // ─────────────────────────────────────────────────────────────────────────
    // BULK SOFT DELETE — used if bulkSoftDelete is added later
    // ─────────────────────────────────────────────────────────────────────────
    @Modifying
    @Query("""
                UPDATE TaxRateConfig t
                SET t.deleted = true, t.enabled = false
                WHERE t.id IN :ids
                  AND t.deleted = false
            """)
    int bulkSoftDelete(@Param("ids") List<UUID> ids);

    // ─────────────────────────────────────────────────────────────────────────
    // CORE FETCH — used by getConfigOrThrow()
    // Excludes soft-deleted records
    // ─────────────────────────────────────────────────────────────────────────
    Optional<TaxRateConfig> findByIdAndDeletedFalse(UUID id);

    // ─────────────────────────────────────────────────────────────────────────
    // EXISTS CHECK — lightweight boolean check (avoids loading the full entity)
    // ─────────────────────────────────────────────────────────────────────────
    boolean existsByStateNameIgnoreCaseAndVehicleSeatingIgnoreCaseAndTaxTypeAndDeletedFalse(
            String stateName,
            String vehicleSeating,
            TaxModeEnum taxType
    );

    // ─────────────────────────────────────────────────────────────────────────
    // UNIQUENESS CHECK — used by validateNoDuplicate()
    // Checks active (non-deleted) records only
    // ─────────────────────────────────────────────────────────────────────────
    Optional<TaxRateConfig> findByStateNameIgnoreCaseAndVehicleSeatingIgnoreCaseAndTaxTypeAndDeletedFalse(
            String stateName,
            String vehicleSeating,
            TaxModeEnum taxType
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Enabled LOOKUP — used by getAllActive() with no taxType filter
    // ─────────────────────────────────────────────────────────────────────────
    List<TaxRateConfig> findAllByEnabledTrueAndDeletedFalse();


    // ─────────────────────────────────────────────────────────────────────────
    // Enabled LOOKUP — used by getAllActive() with taxType filter
    // ─────────────────────────────────────────────────────────────────────────
    List<TaxRateConfig> findAllByEnabledTrueAndDeletedFalseAndTaxType(TaxModeEnum taxType);

    // ─────────────────────────────────────────────
    // Primary rate lookup (used during application submission)
    // ─────────────────────────────────────────────

    /**
     * The single active config for a state + vehicle + tax-type combination.
     * This is the primary lookup when a user submits an application.
     */
    @Query("""
            SELECT t FROM TaxRateConfig t
            WHERE t.stateName      = :stateName
              AND t.vehicleSeating = :vehicleSeating
              AND t.taxType        = :taxType
              AND t.enabled       = true
            """)
    Optional<TaxRateConfig> findActiveConfig(
            @Param("stateName") String stateName,
            @Param("vehicleSeating") String vehicleSeating,
            @Param("taxType") TaxModeEnum taxType);

    // ─────────────────────────────────────────────
    // Filtered listings
    // ─────────────────────────────────────────────

    List<TaxRateConfig> findAllByEnabled(boolean enabled);

    List<TaxRateConfig> findAllByStateNameAndEnabled(String stateName, boolean enabled);

    List<TaxRateConfig> findAllByTaxTypeAndEnabled(TaxModeEnum taxType, boolean enabled);

    List<TaxRateConfig> findAllByStateNameAndTaxTypeAndEnabled(
            String stateName, TaxModeEnum taxType, boolean enabled);

    /**
     * All distinct state names that have at least one active config.
     * Powers the "select state" dropdown on the application form.
     */
    @Query("""
            SELECT DISTINCT t.stateName FROM TaxRateConfig t
            WHERE t.enabled = true
            ORDER BY t.stateName
            """)
    List<String> findAllActiveStateNames();

    /**
     * All distinct vehicle seating categories active for a given state + tax type.
     * Powers the dependent "select vehicle" dropdown.
     */
    @Query("""
            SELECT DISTINCT t.vehicleSeating FROM TaxRateConfig t
            WHERE t.stateName = :stateName
              AND t.taxType   = :taxType
              AND t.enabled  = true
            ORDER BY t.vehicleSeating
            """)
    List<String> findActiveVehicleSeatingsByStateAndTaxType(
            @Param("stateName") String stateName,
            @Param("taxType") TaxModeEnum taxType);

    // ─────────────────────────────────────────────
    // Soft activation / deactivation
    // ─────────────────────────────────────────────

    @Modifying
    @Query("UPDATE TaxRateConfig t SET t.enabled = :status WHERE t.id = :id")
    void setActiveStatus(@Param("id") UUID id, @Param("status") boolean status);

    /**
     * Deactivates all configs for a state — useful when a state is retired.
     */
    @Modifying
    @Query("UPDATE TaxRateConfig t SET t.enabled = false WHERE t.stateName = :stateName")
    int deactivateAllByStateName(@Param("stateName") String stateName);

    // ─────────────────────────────────────────────
    // Bulk rate updates (admin operations)
    // ─────────────────────────────────────────────

    /**
     * Applies a percentage increase/decrease to all daily rates in a state.
     * e.g. factor = 1.10 → 10% increase; 0.95 → 5% reduction.
     */
    @Modifying
    @Query("""
            UPDATE TaxRateConfig t
            SET t.dailyRate    = t.dailyRate    * :factor,
                t.updatedAt    = CURRENT_TIMESTAMP
            WHERE t.stateName  = :stateName
              AND t.enabled   = true
            """)
    int applyDailyRateFactor(
            @Param("stateName") String stateName,
            @Param("factor") BigDecimal factor);

    /**
     * Applies a flat commission override across all payment modes for a config.
     */
    @Modifying
    @Query("""
            UPDATE TaxRateConfig t
            SET t.dailyCommission     = :commission,
                t.weeklyCommission    = :commission,
                t.monthlyCommission   = :commission,
                t.quarterlyCommission = :commission,
                t.yearlyCommission    = :commission,
                t.updatedAt           = CURRENT_TIMESTAMP
            WHERE t.id = :id
            """)
    void setFlatCommissionForAllModes(
            @Param("id") UUID id,
            @Param("commission") BigDecimal commission);

    // ─────────────────────────────────────────────
    // Reporting & audit
    // ─────────────────────────────────────────────

    /**
     * Count of configs grouped by state and active status.
     * Result: Object[]{String stateName, Boolean isActive, Long count}
     */
    @Query("""
            SELECT t.stateName, t.enabled, COUNT(t)
            FROM TaxRateConfig t
            GROUP BY t.stateName, t.enabled
            ORDER BY t.stateName
            """)
    List<Object[]> countConfigsByStateAndStatus();

    /**
     * Configs whose rates are zero — data-quality check.
     */
    @Query("""
            SELECT t FROM TaxRateConfig t
            WHERE t.enabled = true
              AND (t.dailyRate = 0 AND t.weeklyRate = 0
               AND t.monthlyRate = 0 AND t.quarterlyRate = 0
               AND t.yearlyRate = 0)
            """)
    List<TaxRateConfig> findConfigsWithAllZeroRates();

    long countByEnabled(boolean isActive);
}
