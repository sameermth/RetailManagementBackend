package com.retailmanagement.modules.erp.subscription.repository;

import com.retailmanagement.modules.erp.subscription.entity.SubscriptionPlanFeature;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanFeatureRepository extends JpaRepository<SubscriptionPlanFeature, Long> {
    List<SubscriptionPlanFeature> findByPlanIdAndIsEnabledTrue(Long planId);
}
