package com.careos.hms.employee.service;

import com.careos.hms.employee.entity.Employee;
import com.careos.hms.employee.entity.EmployeeStatus;
import com.careos.hms.employee.entity.EmploymentType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class EmployeeSpecification {

    private EmployeeSpecification() {
    }

    public static Specification<Employee> filter(
            Long hospitalId,
            String search,
            Long departmentId,
            Long branchId,
            String designation,
            EmploymentType employmentType,
            EmployeeStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Mandatory tenant and soft-delete filters
            predicates.add(cb.equal(root.get("hospitalId"), hospitalId));
            predicates.add(cb.isFalse(root.get("deleted")));

            // Search filter across text fields
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("firstName")), searchPattern),
                        cb.like(cb.lower(root.get("lastName")), searchPattern),
                        cb.like(cb.lower(root.get("middleName")), searchPattern),
                        cb.like(cb.lower(root.get("employeeNumber")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(cb.lower(root.get("phoneNumber")), searchPattern),
                        cb.like(cb.lower(root.get("designation")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            // Department filter
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            // Branch filter
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }

            // Designation filter
            if (designation != null && !designation.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("designation")), designation.trim().toLowerCase()));
            }

            // Employment type filter
            if (employmentType != null) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType));
            }

            // Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
