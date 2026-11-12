package com.careos.hms.authentication.validator;

import com.careos.hms.common.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 128;
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9\\s]");

    public void validatePasswordStrength(String rawPassword, String username) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new InvalidRequestException("Password must not be blank");
        }
        if (rawPassword.length() < MIN_LENGTH || rawPassword.length() > MAX_LENGTH) {
            throw new InvalidRequestException(
                    String.format("Password must be between %d and %d characters long", MIN_LENGTH, MAX_LENGTH)
            );
        }
        if (!UPPERCASE.matcher(rawPassword).find()
                || !LOWERCASE.matcher(rawPassword).find()
                || !DIGIT.matcher(rawPassword).find()
                || !SPECIAL.matcher(rawPassword).find()) {
            throw new InvalidRequestException(
                    "Password must include uppercase, lowercase, numeric, and special characters"
            );
        }
        if (username != null && username.trim().length() >= 3) {
            String normalizedUser = username.trim().toLowerCase(Locale.ROOT);
            if (rawPassword.toLowerCase(Locale.ROOT).contains(normalizedUser)) {
                throw new InvalidRequestException("Password must not contain your username");
            }
        }
    }
}
