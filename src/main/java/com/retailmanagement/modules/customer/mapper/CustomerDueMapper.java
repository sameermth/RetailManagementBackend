package com.retailmanagement.modules.customer.mapper;

import com.retailmanagement.modules.customer.dto.request.CustomerDueRequest;
import com.retailmanagement.modules.customer.dto.response.CustomerDueResponse;
import com.retailmanagement.modules.customer.enums.DueStatus;
import com.retailmanagement.modules.customer.model.CustomerDue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerDueMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "dueReference", ignore = true)
    @Mapping(target = "originalAmount", source = "amount")
    @Mapping(target = "remainingAmount", source = "amount")
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "reminderCount", ignore = true)
    @Mapping(target = "lastReminderSent", ignore = true)
    @Mapping(target = "lastPaymentDate", ignore = true)
    CustomerDue toEntity(CustomerDueRequest request);

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "daysOverdue", expression = "java(calculateDaysOverdue(customerDue))")
    @Mapping(target = "urgency", expression = "java(determineUrgency(customerDue))")
    CustomerDueResponse toResponse(CustomerDue customerDue);

    List<CustomerDueResponse> toResponseList(List<CustomerDue> customerDues);

    default Integer calculateDaysOverdue(CustomerDue due) {
        if (due.getStatus() == DueStatus.PAID || due.getStatus() == DueStatus.CANCELLED) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        if (today.isAfter(due.getDueDate())) {
            return (int) ChronoUnit.DAYS.between(due.getDueDate(), today);
        }
        return 0;
    }

    default String determineUrgency(CustomerDue due) {
        if (due.getStatus() == DueStatus.PAID || due.getStatus() == DueStatus.CANCELLED) {
            return "NONE";
        }
        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, due.getDueDate());

        if (daysUntilDue < 0) {
            return "OVERDUE";
        } else if (daysUntilDue <= 3) {
            return "HIGH";
        } else if (daysUntilDue <= 7) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}