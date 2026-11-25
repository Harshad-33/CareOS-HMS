package com.careos.hms.employee.repository;

import com.careos.hms.employee.entity.EmployeeLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeLifecycleEventRepository extends JpaRepository<EmployeeLifecycleEvent, Long> {

    List<EmployeeLifecycleEvent> findByHospitalIdAndEmployeeIdOrderByCreatedAtDesc(Long hospitalId, Long employeeId);

    List<EmployeeLifecycleEvent> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
