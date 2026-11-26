package com.careos.hms.hr.leave.service;

import com.careos.hms.common.exception.InvalidRequestException;
import com.careos.hms.common.exception.ResourceNotFoundException;
import com.careos.hms.employee.entity.Employee;
import com.careos.hms.employee.repository.EmployeeRepository;
import com.careos.hms.hr.leave.dto.AdjustLeaveBalanceRequest;
import com.careos.hms.hr.leave.dto.AllocateLeaveBalanceRequest;
import com.careos.hms.hr.leave.dto.LeaveBalanceResponse;
import com.careos.hms.hr.leave.dto.LeaveBalanceTransactionResponse;
import com.careos.hms.hr.leave.entity.LeaveBalance;
import com.careos.hms.hr.leave.entity.LeaveBalanceTransaction;
import com.careos.hms.hr.leave.entity.LeaveTransactionType;
import com.careos.hms.hr.leave.entity.LeaveType;
import com.careos.hms.hr.leave.repository.LeaveBalanceRepository;
import com.careos.hms.hr.leave.repository.LeaveBalanceTransactionRepository;
import com.careos.hms.hr.leave.repository.LeaveTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceTransactionRepository transactionRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveBalanceService(
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveBalanceTransactionRepository transactionRepository,
            LeaveTypeRepository leaveTypeRepository,
            EmployeeRepository employeeRepository
    ) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.employeeRepository = employeeRepository;
    }

    public LeaveBalanceResponse allocateBalance(Long hospitalId, AllocateLeaveBalanceRequest request, String actor) {
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(request.employeePublicId(), hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.employeePublicId()));

        LeaveType leaveType = leaveTypeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(request.leaveTypePublicId(), hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found with ID: " + request.leaveTypePublicId()));

        int year = request.calendarYear();
        BigDecimal allocated = request.allocatedDays();

        LeaveBalance balance = leaveBalanceRepository.findByHospitalIdAndEmployeeIdAndLeaveTypeIdAndCalendarYear(
                hospitalId, employee.getId(), leaveType.getId(), year
        ).orElseGet(() -> {
            LeaveBalance b = new LeaveBalance(hospitalId, employee, leaveType, year, BigDecimal.ZERO);
            return b;
        });

        BigDecimal oldAvailable = balance.getAvailableBalance();
        BigDecimal oldTotal = balance.getTotalAllocated();
        BigDecimal newTotal = oldTotal.add(allocated);
        BigDecimal newAvailable = oldAvailable.add(allocated);

        balance.setTotalAllocated(newTotal);
        balance.setAvailableBalance(newAvailable);

        LeaveBalance savedBalance = leaveBalanceRepository.save(balance);

        // Record in ledger
        LeaveBalanceTransaction tx = new LeaveBalanceTransaction(
                hospitalId,
                employee,
                leaveType,
                LeaveTransactionType.ALLOCATION,
                allocated,
                oldAvailable,
                newAvailable,
                null,
                request.remarks() != null && !request.remarks().isBlank() ? request.remarks() : "Annual leave allocation",
                actor
        );
        transactionRepository.save(tx);

        return LeaveBalanceResponse.from(savedBalance);
    }

    public LeaveBalanceResponse adjustBalance(Long hospitalId, String balancePublicId, AdjustLeaveBalanceRequest request, String actor) {
        LeaveBalance balance = leaveBalanceRepository.findByPublicIdAndHospitalId(balancePublicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found with ID: " + balancePublicId));

        BigDecimal adjustment = request.adjustmentDays();
        BigDecimal oldAvailable = balance.getAvailableBalance();
        BigDecimal newAvailable = oldAvailable.add(adjustment);

        if (newAvailable.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidRequestException("Adjustment would cause negative available leave balance: " + newAvailable);
        }

        BigDecimal newTotal = balance.getTotalAllocated().add(adjustment);
        balance.setTotalAllocated(newTotal);
        balance.setAvailableBalance(newAvailable);

        LeaveBalance saved = leaveBalanceRepository.save(balance);

        LeaveBalanceTransaction tx = new LeaveBalanceTransaction(
                hospitalId,
                balance.getEmployee(),
                balance.getLeaveType(),
                LeaveTransactionType.MANUAL_ADJUSTMENT,
                adjustment,
                oldAvailable,
                newAvailable,
                null,
                request.reason().trim(),
                actor
        );
        transactionRepository.save(tx);

        return LeaveBalanceResponse.from(saved);
    }

    public void reservePendingLeave(Long hospitalId, Long employeeId, Long leaveTypeId, int year, BigDecimal days) {
        LeaveBalance balance = leaveBalanceRepository.findByHospitalIdAndEmployeeIdAndLeaveTypeIdAndCalendarYear(
                hospitalId, employeeId, leaveTypeId, year
        ).orElseThrow(() -> new InvalidRequestException("No leave balance record found for the requested leave type and calendar year " + year));

        if (balance.getAvailableBalance().compareTo(days) < 0) {
            throw new InvalidRequestException("Insufficient leave balance. Available: " + balance.getAvailableBalance() + " days, Requested: " + days + " days.");
        }

        balance.setPendingApprovalDays(balance.getPendingApprovalDays().add(days));
        leaveBalanceRepository.save(balance);
    }

    public void releasePendingLeave(Long hospitalId, Long employeeId, Long leaveTypeId, int year, BigDecimal days) {
        leaveBalanceRepository.findByHospitalIdAndEmployeeIdAndLeaveTypeIdAndCalendarYear(
                hospitalId, employeeId, leaveTypeId, year
        ).ifPresent(balance -> {
            BigDecimal newPending = balance.getPendingApprovalDays().subtract(days);
            if (newPending.compareTo(BigDecimal.ZERO) < 0) {
                newPending = BigDecimal.ZERO;
            }
            balance.setPendingApprovalDays(newPending);
            leaveBalanceRepository.save(balance);
        });
    }

    public void finalizeApprovedLeave(Long hospitalId, Long employeeId, Long leaveTypeId, int year, BigDecimal days, String applicationPublicId, String actor) {
        LeaveBalance balance = leaveBalanceRepository.findByHospitalIdAndEmployeeIdAndLeaveTypeIdAndCalendarYear(
                hospitalId, employeeId, leaveTypeId, year
        ).orElseThrow(() -> new InvalidRequestException("Leave balance not found during final approval"));

        BigDecimal oldAvailable = balance.getAvailableBalance();
        BigDecimal newPending = balance.getPendingApprovalDays().subtract(days);
        if (newPending.compareTo(BigDecimal.ZERO) < 0) {
            newPending = BigDecimal.ZERO;
        }
        balance.setPendingApprovalDays(newPending);
        balance.setUsedDays(balance.getUsedDays().add(days));

        leaveBalanceRepository.save(balance);

        // Record debit transaction in ledger
        LeaveBalanceTransaction tx = new LeaveBalanceTransaction(
                hospitalId,
                balance.getEmployee(),
                balance.getLeaveType(),
                LeaveTransactionType.DEBIT_APPROVED_LEAVE,
                days,
                oldAvailable,
                oldAvailable, // available was already reduced when pending was reserved
                applicationPublicId,
                "Approved leave deduction for application " + applicationPublicId,
                actor
        );
        transactionRepository.save(tx);
    }

    public void reverseApprovedLeave(Long hospitalId, Long employeeId, Long leaveTypeId, int year, BigDecimal days, String applicationPublicId, String actor) {
        LeaveBalance balance = leaveBalanceRepository.findByHospitalIdAndEmployeeIdAndLeaveTypeIdAndCalendarYear(
                hospitalId, employeeId, leaveTypeId, year
        ).orElseThrow(() -> new InvalidRequestException("Leave balance not found during leave cancellation"));

        BigDecimal oldAvailable = balance.getAvailableBalance();
        BigDecimal newAvailable = oldAvailable.add(days);
        BigDecimal newUsed = balance.getUsedDays().subtract(days);
        if (newUsed.compareTo(BigDecimal.ZERO) < 0) {
            newUsed = BigDecimal.ZERO;
        }

        balance.setUsedDays(newUsed);
        balance.setAvailableBalance(newAvailable);
        leaveBalanceRepository.save(balance);

        // Record credit reversal in ledger
        LeaveBalanceTransaction tx = new LeaveBalanceTransaction(
                hospitalId,
                balance.getEmployee(),
                balance.getLeaveType(),
                LeaveTransactionType.CREDIT_REVERSED_LEAVE,
                days,
                oldAvailable,
                newAvailable,
                applicationPublicId,
                "Reversal of approved leave for application " + applicationPublicId,
                actor
        );
        transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getEmployeeBalances(Long hospitalId, String employeePublicId, int year) {
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(employeePublicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeePublicId));

        return leaveBalanceRepository.findByHospitalIdAndEmployeeIdAndCalendarYearWithLeaveType(hospitalId, employee.getId(), year)
                .stream()
                .map(LeaveBalanceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LeaveBalanceTransactionResponse> getEmployeeTransactions(Long hospitalId, String employeePublicId, Pageable pageable) {
        Employee employee = employeeRepository.findByPublicIdAndHospitalIdAndDeletedFalse(employeePublicId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeePublicId));

        return transactionRepository.findByHospitalIdAndEmployeeIdOrderByCreatedAtDesc(hospitalId, employee.getId(), pageable)
                .map(LeaveBalanceTransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getSelfBalances(Long hospitalId, Long employeeId, int year) {
        return leaveBalanceRepository.findByHospitalIdAndEmployeeIdAndCalendarYearWithLeaveType(hospitalId, employeeId, year)
                .stream()
                .map(LeaveBalanceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LeaveBalanceTransactionResponse> getSelfTransactions(Long hospitalId, Long employeeId, Pageable pageable) {
        return transactionRepository.findByHospitalIdAndEmployeeIdOrderByCreatedAtDesc(hospitalId, employeeId, pageable)
                .map(LeaveBalanceTransactionResponse::from);
    }
}
