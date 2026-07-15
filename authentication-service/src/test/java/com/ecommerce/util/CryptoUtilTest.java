package com.ecommerce.util;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CryptoUtilTest {

    @Test
    void encrypt_thenDecrypt_returnsOriginalData() {
        String plaintext = "sensitive-data-123";

        String encrypted = CryptoUtil.encrypt(plaintext);

        assertNotEquals(plaintext, encrypted);
        assertEquals(plaintext, CryptoUtil.decrypt(encrypted));
    }

    @Test
    void hashPassword_thenVerify_succeedsForCorrectPassword() {
        String password = "correct-horse-battery-staple";

        String hash = CryptoUtil.hashPassword(password);

        assertTrue(CryptoUtil.verifyPassword(password, hash));
    }

    @Test
    void verifyPassword_failsForIncorrectPassword() {
        String hash = CryptoUtil.hashPassword("correct-horse-battery-staple");

        assertTrue(!CryptoUtil.verifyPassword("wrong-password", hash));
    }

    @Test
    void hashToken_isDeterministic() {
        String token = "some-refresh-token";

        assertEquals(CryptoUtil.hashToken(token), CryptoUtil.hashToken(token));
    }
}
