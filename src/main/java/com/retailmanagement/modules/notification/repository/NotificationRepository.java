package com.retailmanagement.modules.notification.repository;

import com.retailmanagement.modules.notification.model.Notification;
import com.retailmanagement.modules.notification.enums.NotificationStatus;
import com.retailmanagement.modules.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByNotificationId(String notificationId);

    List<Notification> findByUserId(Long userId);

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    List<Notification> findByCustomerId(Long customerId);

    Page<Notification> findByCustomerId(Long customerId, Pageable pageable);

    List<Notification> findBySupplierId(Long supplierId);

    List<Notification> findByDistributorId(Long distributorId);

    List<Notification> findByType(NotificationType type);

    List<Notification> findByStatus(NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.status = 'PENDING' AND n.scheduledFor <= :now")
    List<Notification> findPendingNotifications(@Param("now") LocalDateTime now);

    @Query("SELECT n FROM Notification n WHERE n.status = 'FAILED' AND n.retryCount < 3")
    List<Notification> findFailedNotificationsToRetry();

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.readAt IS NULL")
    Long countUnreadByUser(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.referenceType = :referenceType AND n.referenceId = :referenceId")
    List<Notification> findByReference(@Param("referenceType") String referenceType,
                                       @Param("referenceId") Long referenceId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now WHERE n.id = :id")
    void markAsRead(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now WHERE n.userId = :userId")
    void markAllAsReadByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    boolean existsByNotificationId(String notificationId);
}