package com.careos.hms.role.repository;

import com.careos.hms.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCodeAndDeletedFalseAndActiveTrue(String code);
}
