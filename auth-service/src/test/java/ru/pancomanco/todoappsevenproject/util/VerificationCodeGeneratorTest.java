package ru.pancomanco.todoappsevenproject.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class VerificationCodeGeneratorTest {

    @Test
    void getRandomVerificationCode_ShouldBeExactlySixDigits() {
        String code = VerificationCodeGenerator.getRandomVerificationCode();

        assertThat(code)
                .as("Must be 6 digits")
                .hasSize(6)
                .as("Code must have just numbers")
                .matches("\\d{6}");
    }

    @Test
    void getRandomVerificationCode_ShouldNotBeHardcoded() {
        String code1 = VerificationCodeGenerator.getRandomVerificationCode();
        String code2 = VerificationCodeGenerator.getRandomVerificationCode();

        assertThat(code1).isNotEqualTo(code2);
    }


}
