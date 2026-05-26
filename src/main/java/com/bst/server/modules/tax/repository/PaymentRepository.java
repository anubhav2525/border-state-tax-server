package com.bst.server.modules.tax.repository;

import com.bst.server.modules.tax.data.entities.Payment;
import com.bst.server.modules.tax.data.enums.PayStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Used during Razorpay callback verification
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    // Used to check if a SUCCESS payment already exists (defensive guard)
    boolean existsByApplicationIdAndStatus(UUID applicationId, PayStatusEnum status);

    // Fetch all payment attempts for an application (for admin view)
    @Query("SELECT p FROM Payment p WHERE p.application.id = :applicationId ORDER BY p.createdAt DESC")
    List<Payment> findAllByApplicationId(@Param("applicationId") UUID applicationId);
}
