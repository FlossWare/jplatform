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

package org.flossware.platform.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnvironmentVariableResolver.
 * Verifies environment variable substitution for secure credential handling.
 */
class EnvironmentVariableResolverTest {

    private EnvironmentVariableResolver resolver;
    private Map<String, String> testEnv;

    @BeforeEach
    void setUp() {
        testEnv = new HashMap<>();
        testEnv.put("TEST_VAR", "test_value");
        testEnv.put("DATABASE_URL", "jdbc:postgresql://localhost:5432/mydb");
        testEnv.put("API_KEY", "sk_live_abc123xyz");
        testEnv.put("PASSWORD", "SuperSecret123!");
        testEnv.put("EMPTY_VAR", "");

        resolver = new EnvironmentVariableResolver(testEnv, false);
    }

    @Test
    void testBracedVariableResolution() {
        String input = "${TEST_VAR}";
        String result = resolver.resolve(input);
        assertEquals("test_value", result);
    }

    @Test
    void testSimpleVariableResolution() {
        String input = "$TEST_VAR";
        String result = resolver.resolve(input);
        assertEquals("test_value", result);
    }

    @Test
    void testVariableWithDefaultValue() {
        String input = "${MISSING_VAR:default_value}";
        String result = resolver.resolve(input);
        assertEquals("default_value", result);
    }

    @Test
    void testVariableWithDefaultValueWhenExists() {
        String input = "${TEST_VAR:default_value}";
        String result = resolver.resolve(input);
        assertEquals("test_value", result, "Should use actual value, not default");
    }

    @Test
    void testMultipleVariablesInString() {
        String input = "url=${DATABASE_URL} key=${API_KEY}";
        String result = resolver.resolve(input);
        assertEquals("url=jdbc:postgresql://localhost:5432/mydb key=sk_live_abc123xyz", result);
    }

    @Test
    void testMixedBracedAndSimpleVariables() {
        String input = "${DATABASE_URL} and $API_KEY";
        String result = resolver.resolve(input);
        assertTrue(result.contains("jdbc:postgresql://localhost:5432/mydb"));
        assertTrue(result.contains("sk_live_abc123xyz"));
    }

    @Test
    void testMissingVariableWithoutDefault() {
        String input = "${MISSING_VAR}";
        String result = resolver.resolve(input);
        assertEquals("${MISSING_VAR}", result, "Should leave unreplaced when no default");
    }

    @Test
    void testMissingVariableWithFailOnMissing() {
        EnvironmentVariableResolver strictResolver = new EnvironmentVariableResolver(testEnv, true);
        String input = "${MISSING_VAR}";

        assertThrows(IllegalArgumentException.class, () -> {
            strictResolver.resolve(input);
        }, "Should throw when variable missing and failOnMissing=true");
    }

    @Test
    void testNullInputReturnsNull() {
        String result = resolver.resolve(null);
        assertNull(result);
    }

    @Test
    void testEmptyInputReturnsEmpty() {
        String result = resolver.resolve("");
        assertEquals("", result);
    }

    @Test
    void testStringWithoutVariables() {
        String input = "No variables here";
        String result = resolver.resolve(input);
        assertEquals("No variables here", result);
    }

    @Test
    void testEmptyVariableValue() {
        String input = "${EMPTY_VAR}";
        String result = resolver.resolve(input);
        assertEquals("", result);
    }

    @Test
    void testVariableInMiddleOfString() {
        String input = "prefix_${TEST_VAR}_suffix";
        String result = resolver.resolve(input);
        assertEquals("prefix_test_value_suffix", result);
    }

    @Test
    void testConsecutiveVariables() {
        String input = "${TEST_VAR}${API_KEY}";
        String result = resolver.resolve(input);
        assertEquals("test_valuesk_live_abc123xyz", result);
    }

    @Test
    void testDefaultValueWithSpecialCharacters() {
        String input = "${MISSING:default:with:colons}";
        String result = resolver.resolve(input);
        assertEquals("default:with:colons", result);
    }

    @Test
    void testDefaultValueWithSpaces() {
        String input = "${MISSING:default with spaces}";
        String result = resolver.resolve(input);
        assertEquals("default with spaces", result);
    }

    @Test
    void testResolveMap() {
        Map<String, String> input = new HashMap<>();
        input.put("database.url", "${DATABASE_URL}");
        input.put("database.password", "${PASSWORD}");
        input.put("api.key", "${API_KEY}");
        input.put("static.value", "no_variables");

        Map<String, String> result = resolver.resolveMap(input);

        assertEquals("jdbc:postgresql://localhost:5432/mydb", result.get("database.url"));
        assertEquals("SuperSecret123!", result.get("database.password"));
        assertEquals("sk_live_abc123xyz", result.get("api.key"));
        assertEquals("no_variables", result.get("static.value"));
    }

    @Test
    void testResolveNullMap() {
        Map<String, String> result = resolver.resolveMap(null);
        assertNull(result);
    }

    @Test
    void testResolveEmptyMap() {
        Map<String, String> result = resolver.resolveMap(new HashMap<>());
        assertTrue(result.isEmpty());
    }

    @Test
    void testContainsVariablesBraced() {
        assertTrue(EnvironmentVariableResolver.containsVariables("${VAR}"));
        assertTrue(EnvironmentVariableResolver.containsVariables("prefix ${VAR} suffix"));
    }

    @Test
    void testContainsVariablesSimple() {
        assertTrue(EnvironmentVariableResolver.containsVariables("$VAR"));
        assertTrue(EnvironmentVariableResolver.containsVariables("prefix $VAR suffix"));
    }

    @Test
    void testContainsVariablesNone() {
        assertFalse(EnvironmentVariableResolver.containsVariables("no variables"));
        assertFalse(EnvironmentVariableResolver.containsVariables(""));
        assertFalse(EnvironmentVariableResolver.containsVariables(null));
    }

    @Test
    void testContainsVariablesFalsePositive() {
        assertFalse(EnvironmentVariableResolver.containsVariables("price is $5"));
        assertFalse(EnvironmentVariableResolver.containsVariables("${"));
    }

    @Test
    void testVariableNameWithUnderscores() {
        testEnv.put("MY_VAR_NAME", "value");
        resolver = new EnvironmentVariableResolver(testEnv, false);

        String result = resolver.resolve("${MY_VAR_NAME}");
        assertEquals("value", result);
    }

    @Test
    void testVariableNameWithNumbers() {
        testEnv.put("VAR123", "value");
        resolver = new EnvironmentVariableResolver(testEnv, false);

        String result = resolver.resolve("${VAR123}");
        assertEquals("value", result);
    }

    @Test
    void testRealWorldDatabaseConfig() {
        String config = "jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME}";
        testEnv.put("DB_NAME", "production_db");
        resolver = new EnvironmentVariableResolver(testEnv, false);

        String result = resolver.resolve(config);
        assertEquals("jdbc:postgresql://localhost:5432/production_db", result);
    }

    @Test
    void testRealWorldApiKeyConfig() {
        String config = "Authorization: Bearer ${API_TOKEN}";
        testEnv.put("API_TOKEN", "token_abc123");
        resolver = new EnvironmentVariableResolver(testEnv, false);

        String result = resolver.resolve(config);
        assertEquals("Authorization: Bearer token_abc123", result);
    }

    @Test
    void testPasswordWithSpecialCharacters() {
        testEnv.put("COMPLEX_PASSWORD", "P@ssw0rd!#$%^&*()");
        resolver = new EnvironmentVariableResolver(testEnv, false);

        String result = resolver.resolve("${COMPLEX_PASSWORD}");
        assertEquals("P@ssw0rd!#$%^&*()", result);
    }

    @Test
    void testUrlWithQueryParameters() {
        String url = "https://api.example.com/endpoint?key=${API_KEY}&secret=${SECRET:default}";
        testEnv.put("API_KEY", "my_api_key");
        resolver = new EnvironmentVariableResolver(testEnv, false);

        String result = resolver.resolve(url);
        assertEquals("https://api.example.com/endpoint?key=my_api_key&secret=default", result);
    }

    @Test
    void testNestedBraces() {
        // Should not support nested variables (e.g., ${${VAR}})
        String input = "${${TEST_VAR}}";
        String result = resolver.resolve(input);
        // Should treat as literal text since nesting is not supported
        assertNotEquals("test_value", result);
    }

    @Test
    void testSystemEnvironmentVariableAccess() {
        // Use default constructor (no overrides)
        EnvironmentVariableResolver systemResolver = new EnvironmentVariableResolver();

        // PATH should exist on all systems
        String result = systemResolver.resolve("${PATH:not_found}");
        assertNotEquals("not_found", result, "PATH environment variable should exist");
    }
}
