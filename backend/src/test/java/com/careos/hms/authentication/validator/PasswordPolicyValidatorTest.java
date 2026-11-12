package com.careos.hms.authentication.validator;

import com.careos.hms.common.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyValidatorTest {

    private final PasswordPolicyValidator validator = new PasswordPolicyValidator();

    @Test
    void shouldAcceptCompliantPassword() {
        assertThatCode(() -> validator.validatePasswordStrength("Care0s#Secur3Pass!", "hospital.admin"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "Short1!",
            "alllowercase123!",
            "ALLUPPERCASE123!",
            "NoDigitsHere!!",
            "NoSpecialChar1234",
            "Myhospital.admin#123"
    })
    void shouldRejectNonCompliantPasswords(String candidate) {
        assertThatThrownBy(() -> validator.validatePasswordStrength(candidate, "hospital.admin"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
