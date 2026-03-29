package com.retailmanagement.modules.notification.mapper;

import com.retailmanagement.modules.notification.dto.response.NotificationResponse;
import com.retailmanagement.modules.notification.model.Notification;
import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.erp.party.repository.CustomerRepository;
import com.retailmanagement.modules.erp.party.repository.SupplierRepository;
import com.retailmanagement.modules.erp.party.repository.DistributorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final DistributorRepository distributorRepository;

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        NotificationResponse response = NotificationResponse.builder()
                .id(notification.getId())
                .notificationId(notification.getNotificationId())
                .userId(notification.getUserId())
                .customerId(notification.getCustomerId())
                .supplierId(notification.getSupplierId())
                .distributorId(notification.getDistributorId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .status(notification.getStatus())
                .priority(notification.getPriority())
                .title(notification.getTitle())
                .content(notification.getContent())
                .recipient(notification.getRecipient())
                .sender(notification.getSender())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .scheduledFor(notification.getScheduledFor())
                .retryCount(notification.getRetryCount())
                .errorMessage(notification.getErrorMessage())
                .createdAt(notification.getCreatedAt())
                .build();

        // Fetch names for related entities
        if (notification.getUserId() != null) {
            userRepository.findById(notification.getUserId())
                    .ifPresent(user -> response.setUserName(user.getUsername()));
        }

        if (notification.getCustomerId() != null) {
            customerRepository.findById(notification.getCustomerId())
                    .ifPresent(customer -> response.setCustomerName(customer.getFullName()));
        }

        if (notification.getSupplierId() != null) {
            supplierRepository.findById(notification.getSupplierId())
                    .ifPresent(supplier -> response.setSupplierName(supplier.getName()));
        }

        if (notification.getDistributorId() != null) {
            distributorRepository.findById(notification.getDistributorId())
                    .ifPresent(distributor -> response.setDistributorName(distributor.getName()));
        }

        return response;
    }

    public List<NotificationResponse> toResponseList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
