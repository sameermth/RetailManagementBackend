package com.retailmanagement.common.validation;

import com.retailmanagement.modules.auth.repository.UserRepository;
import com.retailmanagement.modules.customer.repository.CustomerRepository;
import com.retailmanagement.modules.supplier.repository.SupplierRepository;
import com.retailmanagement.modules.distributor.repository.DistributorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return true;
        }

        boolean exists = userRepository.existsByEmail(email) ||
                customerRepository.existsByEmail(email) ||
                supplierRepository.existsByEmail(email) ||
                distributorRepository.existsByEmail(email);

        return !exists;
    }
}