package com.nyala.server.infrastructure.mail.oauth2.google;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GoogleOAuth2CredTest {

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "2, 4",
            "3, 9"
    })
    public void testSquares(Integer input, Integer expected) {
        assertEquals(expected, input * input);
    }

    @ParameterizedTest
    @CsvSource({"test,TEST", "tEst,TEST", "Java,JAVA"})
    public void toUpperCase_ShouldGenerateTheExpectedUppercaseValue(String input, String expected) {
        String actualValue = input.toUpperCase();
        assertEquals(expected, actualValue);
    }

    @Test
    public void toUppercase() {
        String input = "java";
        String expected = "JAVA";
        String actualValue = input.toUpperCase();
        assertEquals(expected, actualValue);
    }

}
