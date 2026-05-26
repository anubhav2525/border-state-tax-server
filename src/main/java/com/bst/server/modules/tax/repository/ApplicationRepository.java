package com.bst.server.modules.tax.repository;

import com.bst.server.modules.tax.data.entities.Application;
import com.bst.server.modules.tax.data.enums.AppStatusEnum;
import com.bst.server.modules.tax.data.enums.PaymentModeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository
        extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

    // Used by admin to count unread applications
    long countByIsReadFalseAndStatus(AppStatusEnum status);

    // Fetch with taxRateConfig eagerly (avoids N+1 on detail page)
    @Query("SELECT a FROM Application a JOIN FETCH a.taxRateConfig WHERE a.id = :id")
    Optional<Application> findByIdWithConfig(@Param("id") UUID id);

    // Check duplicate DRAFT for same vehicle + mode to prevent double submissions
    boolean existsByVehicleNumberAndPaymentModeAndStatus(
            String vehicleNumber,
            PaymentModeEnum paymentMode,
            AppStatusEnum status
    );

    // Admin: mark all as read in bulk (optional utility)
    @Modifying
    @Query("UPDATE Application a SET a.isRead = true WHERE a.isRead = false AND a.status = :status")
    int markAllAsReadByStatus(@Param("status") AppStatusEnum status);


    // ADDED: countApplicationStatus() — total count per status (all read + unread)
    long countByStatus(AppStatusEnum status);

}