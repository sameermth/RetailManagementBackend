package com.retailmanagement.modules.auth.repository;

import com.retailmanagement.modules.auth.model.Person;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findFirstByPrimaryEmailIgnoreCase(String primaryEmail);
    Optional<Person> findFirstByPrimaryPhone(String primaryPhone);
}
