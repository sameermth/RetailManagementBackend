package com.retailmanagement.modules.erp.subscription.repository;

import com.retailmanagement.modules.erp.subscription.entity.SubscriptionFeature;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, Long> {
    Optional<SubscriptionFeature> findByCode(String code);
    List<SubscriptionFeature> findByCodeIn(Collection<String> codes);
}
