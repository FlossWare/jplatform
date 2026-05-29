/*
 * Copyright (C) 2024-2026 FlossWare
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.flossware.platform.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MaskingPatternLayout.
 * Verifies that sensitive data is properly redacted from log messages.
 */
class MaskingPatternLayoutTest {

    private MaskingPatternLayout layout;
    private LoggerContext loggerContext;
    private Logger logger;

    @BeforeEach
    void setUp() {
        loggerContext = new LoggerContext();
        logger = loggerContext.getLogger("test");
        layout = new MaskingPatternLayout();
        layout.setContext(loggerContext);
        layout.setPattern("%msg%n");
        layout.start();
    }

    @Test
    void testApiKeyMaskingInJson() {
        layout.addMaskPattern("\"apiKey\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("Config: {\"apiKey\": \"sk_live_abc123xyz\"}");

        String result = layout.doLayout(event);

        assertFalse(result.contains("sk_live_abc123xyz"), "API key should be masked");
        assertTrue(result.contains("***REDACTED***"), "Should contain redaction marker");
    }

    @Test
    void testPasswordMaskingInJson() {
        layout.addMaskPattern("\"password\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("User login: {\"username\": \"admin\", \"password\": \"SuperSecret123!\"}");

        String result = layout.doLayout(event);

        assertFalse(result.contains("SuperSecret123!"), "Password should be masked");
        assertTrue(result.contains("***REDACTED***"), "Should contain redaction marker");
        assertTrue(result.contains("admin"), "Username should not be masked");
    }

    @Test
    void testBearerTokenMasking() {
        layout.addMaskPattern("Authorization:\\s*Bearer\\s+([A-Za-z0-9._-]+)");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("HTTP Header: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");

        String result = layout.doLayout(event);

        assertFalse(result.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"),
                "Bearer token should be masked");
        assertTrue(result.contains("***REDACTED***"), "Should contain redaction marker");
    }

    @Test
    void testApiKeyHeaderMasking() {
        layout.addMaskPattern("X-API-Key:\\s+([A-Za-z0-9._-]+)");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("Request headers: X-API-Key: ak_12345abcdef");

        String result = layout.doLayout(event);

        assertFalse(result.contains("ak_12345abcdef"), "API key header should be masked");
        assertTrue(result.contains("***REDACTED***"), "Should contain redaction marker");
    }

    @Test
    void testMultipleSecretsInSameMessage() {
        layout.addMaskPattern("\"apiKey\"\\s*:\\s*\"([^\"]+)\"");
        layout.addMaskPattern("\"password\"\\s*:\\s*\"([^\"]+)\"");
        layout.addMaskPattern("\"token\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("Credentials: {\"apiKey\": \"key123\", \"password\": \"pass456\", \"token\": \"tok789\"}");

        String result = layout.doLayout(event);

        assertFalse(result.contains("key123"), "API key should be masked");
        assertFalse(result.contains("pass456"), "Password should be masked");
        assertFalse(result.contains("tok789"), "Token should be masked");
        assertTrue(result.contains("***REDACTED***"), "Should contain redaction markers");
    }

    @Test
    void testNoMaskingWhenNoPatternMatches() {
        layout.addMaskPattern("\"password\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("Normal log message without secrets");

        String result = layout.doLayout(event);

        assertEquals("Normal log message without secrets\n", result,
                "Message without secrets should be unchanged");
    }

    @Test
    void testNullMessageHandling() {
        layout.addMaskPattern("\"password\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage(null);

        String result = layout.doLayout(event);

        assertNotNull(result, "Should handle null message gracefully");
    }

    @Test
    void testEmptyPatternIsIgnored() {
        layout.addMaskPattern("");
        layout.addMaskPattern(null);

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("Test message");

        String result = layout.doLayout(event);

        assertEquals("Test message\n", result, "Empty/null patterns should be ignored");
    }

    @Test
    void testKeyValueFormatMasking() {
        layout.addMaskPattern("apiKey=([^\\s,&]+)");
        layout.addMaskPattern("password=([^\\s,&]+)");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("Query params: apiKey=secret123&password=hidden456&user=admin");

        String result = layout.doLayout(event);

        assertFalse(result.contains("secret123"), "API key value should be masked");
        assertFalse(result.contains("hidden456"), "Password value should be masked");
        assertTrue(result.contains("admin"), "Non-sensitive values should remain");
    }

    @Test
    void testCaseVariationsMasking() {
        layout.addMaskPattern("\"apiKey\"\\s*:\\s*\"([^\"]+)\"");
        layout.addMaskPattern("\"api_key\"\\s*:\\s*\"([^\"]+)\"");
        layout.addMaskPattern("\"API_KEY\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("{\"apiKey\": \"k1\", \"api_key\": \"k2\", \"API_KEY\": \"k3\"}");

        String result = layout.doLayout(event);

        assertFalse(result.contains("k1"), "apiKey should be masked");
        assertFalse(result.contains("k2"), "api_key should be masked");
        assertFalse(result.contains("k3"), "API_KEY should be masked");
    }

    @Test
    void testPatternWithoutCaptureGroup() {
        layout.addMaskPattern("SECRET");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("This is a SECRET message");

        String result = layout.doLayout(event);

        assertFalse(result.contains("SECRET"), "Pattern match should be replaced");
        assertTrue(result.contains("***REDACTED***"), "Should contain redaction marker");
    }

    @Test
    void testGetMaskPatterns() {
        layout.addMaskPattern("pattern1");
        layout.addMaskPattern("pattern2");

        assertEquals(2, layout.getMaskPatterns().size(),
                "Should have 2 patterns configured");
    }

    @Test
    void testAccessTokenMasking() {
        layout.addMaskPattern("\"access_token\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("OAuth response: {\"access_token\": \"ya29.a0AfH6SMB...\", \"expires_in\": 3600}");

        String result = layout.doLayout(event);

        assertFalse(result.contains("ya29.a0AfH6SMB..."), "Access token should be masked");
        assertTrue(result.contains("3600"), "Non-sensitive data should remain");
    }

    @Test
    void testClientSecretMasking() {
        layout.addMaskPattern("\"client_secret\"\\s*:\\s*\"([^\"]+)\"");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLevel(Level.INFO);
        event.setMessage("OAuth config: {\"client_id\": \"app123\", \"client_secret\": \"cs_secret456\"}");

        String result = layout.doLayout(event);

        assertFalse(result.contains("cs_secret456"), "Client secret should be masked");
        assertTrue(result.contains("app123"), "Client ID should remain visible");
    }
}
