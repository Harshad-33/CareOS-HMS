package com.careos.hms.employee.service;

import com.careos.hms.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmployeeCodeGeneratorService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^EMP-\\d{4}-(\\d+)$");

    private final EmployeeRepository employeeRepository;

    public EmployeeCodeGeneratorService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public synchronized String generateNextEmployeeNumber(Long hospitalId) {
        int currentYear = LocalDate.now().getYear();
        String prefix = String.format("EMP-%d-", currentYear);

        List<String> existingNumbers = employeeRepository.findEmployeeNumbersByPrefix(hospitalId, prefix);

        int maxSeq = 0;
        for (String num : existingNumbers) {
            if (num != null) {
                Matcher matcher = CODE_PATTERN.matcher(num.trim());
                if (matcher.matches()) {
                    try {
                        int seq = Integer.parseInt(matcher.group(1));
                        if (seq > maxSeq) {
                            maxSeq = seq;
                        }
                    } catch (NumberFormatException ignored) {
                        // ignore malformed sequence numbers
                    }
                }
            }
        }

        int nextSeq = maxSeq + 1;
        String candidateCode;
        do {
            candidateCode = String.format("EMP-%d-%04d", currentYear, nextSeq);
            nextSeq++;
        } while (employeeRepository.existsByHospitalIdAndEmployeeNumberIgnoreCaseAndDeletedFalse(hospitalId, candidateCode));

        return candidateCode;
    }
}
