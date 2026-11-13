package com.careos.hms.department.repository;

import com.careos.hms.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByHospitalIdAndDeletedFalseOrderByDepartmentNameAsc(Long hospitalId);

    Optional<Department> findByPublicIdAndDeletedFalse(String publicId);

    Optional<Department> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);

    boolean existsByHospitalIdAndDepartmentCodeIgnoreCaseAndDeletedFalse(Long hospitalId, String departmentCode);

    boolean existsByHospitalIdAndDepartmentCodeIgnoreCaseAndIdNotAndDeletedFalse(Long hospitalId, String departmentCode, Long id);
}
