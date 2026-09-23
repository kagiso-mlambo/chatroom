package common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ANSICodesTest {

    @Test
    void everyCodeIsNonEmpty() {
        for (ANSICodes code : ANSICodes.values()) {
            assertFalse(code.code().isEmpty(), code.name() + " should not be empty");
        }
    }

    @Test
    void resetUsesTheStandardResetSequence() {
        assertEquals("\u001B[0m", ANSICodes.RESET.code());
    }

    @Test
    void everyCodeIsDistinct() {
        ANSICodes[] values = ANSICodes.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i].code(), values[j].code(),
                        values[i].name() + " and " + values[j].name() + " should differ");
            }
        }
    }
}
