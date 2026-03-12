package com.retailmanagement.modules.notification.repository;

import com.retailmanagement.modules.notification.model.NotificationTemplate;
import com.retailmanagement.modules.notification.enums.NotificationType;
import com.retailmanagement.modules.notification.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateCode(String templateCode);

    List<NotificationTemplate> findByType(NotificationType type);

    List<NotificationTemplate> findByChannel(NotificationChannel channel);

    List<NotificationTemplate> findByIsActiveTrue();

    Optional<NotificationTemplate> findByTypeAndChannel(NotificationType type, NotificationChannel channel);

    boolean existsByTemplateCode(String templateCode);
}