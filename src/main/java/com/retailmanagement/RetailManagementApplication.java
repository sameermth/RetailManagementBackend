package com.retailmanagement;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(excludeFilters = {
        @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.retailmanagement\\.modules\\.(customer|distributor|inventory|product|purchase|sales|supplier)(\\..*)?"
        )
})
@EnableJpaRepositories(basePackages = {
        "com.retailmanagement.modules.auth.repository",
        "com.retailmanagement.modules.notification.repository",
        "com.retailmanagement.modules.report.repository",
        "com.retailmanagement.modules.erp"
})
@EntityScan(basePackages = {
        "com.retailmanagement.modules.auth.model",
        "com.retailmanagement.modules.notification.model",
        "com.retailmanagement.modules.report.model",
        "com.retailmanagement.modules.erp"
})
@EnableScheduling
@EnableCaching
public class RetailManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(RetailManagementApplication.class, args);
    }
}
