package com.careos.hms.employee.repository;

import com.careos.hms.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByPublicIdAndDeletedFalse(String publicId);

    Optional<Employee> findByPublicIdAndHospitalIdAndDeletedFalse(String publicId, Long hospitalId);

    @Query("""
            SELECT DISTINCT e FROM Employee e
            LEFT JOIN FETCH e.department
            LEFT JOIN FETCH e.branch
            LEFT JOIN FETCH e.reportingManager
            LEFT JOIN FETCH e.user u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role
            WHERE e.publicId = :publicId
              AND e.hospitalId = :hospitalId
              AND e.deleted = false
            """)
    Optional<Employee> findWithDetailsByPublicIdAndHospitalId(@Param("publicId") String publicId, @Param("hospitalId") Long hospitalId);

    @Query("""
            SELECT DISTINCT e FROM Employee e
            LEFT JOIN FETCH e.department
            LEFT JOIN FETCH e.branch
            LEFT JOIN FETCH e.reportingManager
            LEFT JOIN FETCH e.user u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role
            WHERE e.publicId = :publicId
              AND e.deleted = false
            """)
    Optional<Employee> findWithDetailsByPublicId(@Param("publicId") String publicId);

    Optional<Employee> findByHospitalIdAndEmployeeNumberAndDeletedFalse(Long hospitalId, String employeeNumber);

    Optional<Employee> findByUserIdAndDeletedFalse(Long userId);

    boolean existsByHospitalIdAndEmployeeNumberIgnoreCaseAndDeletedFalse(Long hospitalId, String employeeNumber);

    boolean existsByHospitalIdAndEmailIgnoreCaseAndDeletedFalse(Long hospitalId, String email);

    boolean existsByHospitalIdAndEmployeeNumberIgnoreCaseAndIdNotAndDeletedFalse(Long hospitalId, String employeeNumber, Long id);

    boolean existsByHospitalIdAndEmailIgnoreCaseAndIdNotAndDeletedFalse(Long hospitalId, String email, Long id);

    boolean existsByUserIdAndIdNotAndDeletedFalse(Long userId, Long id);

    long countByHospitalIdAndDeletedFalse(Long hospitalId);

    @Query("SELECT e.employeeNumber FROM Employee e WHERE e.hospitalId = :hospitalId AND e.employeeNumber LIKE CONCAT(:prefix, '%')")
    java.util.List<String> findEmployeeNumbersByPrefix(@Param("hospitalId") Long hospitalId, @Param("prefix") String prefix);

    @Query("SELECT MAX(CAST(SUBSTRING(e.employeeNumber, LENGTH(:prefix) + 1) AS int)) " +
           "FROM Employee e WHERE e.hospitalId = :hospitalId AND e.employeeNumber LIKE CONCAT(:prefix, '%')")
    Integer findMaxEmployeeSequenceNumber(@Param("hospitalId") Long hospitalId, @Param("prefix") String prefix);
}
