package ru.pancomanco.authservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class HashUtilTest {

    @Test
    void sha256Hex_ValidInput_ReturnsHexString() {
        String hash = HashUtil.sha256Hex("hello");

        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[a-f0-9]+");

        assertThat(hash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    void sha256Hex_NullInput_ThrowsException() {
        assertThatThrownBy(() -> HashUtil.sha256Hex(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value to hash must not be null");
    }


    @Test
    void sha256Base64Url_ValidInput_ReturnsBase64UrlString() {
        String hash = HashUtil.sha256Base64Url("hello");

        assertThat(hash).hasSize(43);

        assertThat(hash).doesNotContain("+", "/", "=");
        assertThat(hash).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void sha256Base64Url_NullInput_ThrowsException() {
        assertThatThrownBy(() -> HashUtil.sha256Base64Url(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Value to hash must not be null");
    }


    @Test
    void sha256Base64UrlNormalizedEmail_NormalizesBeforeHashing() {
        String rawEmail = "  Test.User@Example.COM  ";
        String normalizedEmail = "test.user@example.com";

        String hashFromRaw = HashUtil.sha256Base64UrlNormalizedEmail(rawEmail);
        String hashFromNormalized = HashUtil.sha256Base64Url(normalizedEmail);

        assertThat(hashFromRaw)
                .as("Hash of raw email must match hash of normalized email")
                .isEqualTo(hashFromNormalized);
    }

    @Test
    void sha256Base64UrlNormalizedEmail_NullInput_ThrowsException() {
        assertThatThrownBy(() -> HashUtil.sha256Base64UrlNormalizedEmail(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email to hash must not be null");
    }

    @Test
    void hashing_IsDeterministic() {
        String input = "test";

        String hash1 = HashUtil.sha256Hex(input);
        String hash2 = HashUtil.sha256Hex(input);
        String hash3 = HashUtil.sha256Base64Url(input);
        String hash4 = HashUtil.sha256Base64Url(input);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash3).isEqualTo(hash4);
    }

    @Test
    void hashing_DifferentInputs_DifferentOutputs() {
        String hash1 = HashUtil.sha256Hex("password1");
        String hash2 = HashUtil.sha256Hex("password2");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
