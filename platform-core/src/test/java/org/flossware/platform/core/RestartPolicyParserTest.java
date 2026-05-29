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

package org.flossware.platform.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.stream.Stream;

import org.flossware.platform.api.ApplicationDescriptor;
import org.flossware.platform.api.RestartPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests for RestartPolicyParser. */
@Tag("unit")
class RestartPolicyParserTest {

  private RestartPolicyParser parser;

  @BeforeEach
  void setUp() {
    parser = new RestartPolicyParser();
  }

  @Test
  void testParseNoPolicy() {
    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .build();

    Optional<RestartPolicy> result = parser.parse(descriptor);

    assertFalse(result.isPresent(), "Should return empty when no restart policy configured");
  }

  @ParameterizedTest
  @CsvSource({
    "never, NEVER, 0",
    "always, ALWAYS, " + Integer.MAX_VALUE,
    "on-failure, ON_FAILURE, 5"
  })
  void testParsePolicyTypes(
      String policyValue,
      RestartPolicy.RestartCondition expectedCondition,
      int expectedMaxRetries) {
    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("restart.policy", policyValue)
            .build();

    Optional<RestartPolicy> result = parser.parse(descriptor);

    assertTrue(result.isPresent());
    assertEquals(expectedCondition, result.get().getCondition());
    if (expectedMaxRetries > 0) {
      assertEquals(expectedMaxRetries, result.get().getMaxRetries());
    }
  }

  @ParameterizedTest
  @CsvSource({"'', 5, 5, 300", "10, 10, 15, 600"})
  void testParseOnFailureWithProperties(
      String maxRetries,
      int expectedMaxRetries,
      int expectedInitialBackoff,
      int expectedMaxBackoff) {
    ApplicationDescriptor.Builder builder =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("restart.policy", "on-failure");

    if (!maxRetries.isEmpty()) {
      builder
          .property("restart.maxRetries", maxRetries)
          .property("restart.initialBackoff", "15")
          .property("restart.maxBackoff", "600");
    }

    Optional<RestartPolicy> result = parser.parse(builder.build());

    assertTrue(result.isPresent());
    RestartPolicy policy = result.get();
    assertEquals(RestartPolicy.RestartCondition.ON_FAILURE, policy.getCondition());
    assertEquals(expectedMaxRetries, policy.getMaxRetries());
    assertEquals(expectedInitialBackoff, policy.getInitialBackoffSeconds());
    assertEquals(expectedMaxBackoff, policy.getMaxBackoffSeconds());
  }

  @ParameterizedTest
  @CsvSource({"ON-FAILURE, ON_FAILURE", "unknown-policy, NEVER"})
  void testParsePolicyEdgeCases(
      String policyValue, RestartPolicy.RestartCondition expectedCondition) {
    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("restart.policy", policyValue)
            .build();

    Optional<RestartPolicy> result = parser.parse(descriptor);

    assertTrue(result.isPresent());
    assertEquals(expectedCondition, result.get().getCondition());
  }

  @ParameterizedTest
  @MethodSource("provideInvalidPropertyCases")
  void testParseInvalidPropertiesUseDefaults(
      String maxRetries,
      String initialBackoff,
      String maxBackoff,
      int expectedMaxRetries,
      int expectedInitialBackoff,
      int expectedMaxBackoff) {
    ApplicationDescriptor.Builder builder =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("restart.policy", "on-failure");

    if (maxRetries != null) {
      builder.property("restart.maxRetries", maxRetries);
    }
    if (initialBackoff != null) {
      builder.property("restart.initialBackoff", initialBackoff);
    }
    if (maxBackoff != null) {
      builder.property("restart.maxBackoff", maxBackoff);
    }

    Optional<RestartPolicy> result = parser.parse(builder.build());

    assertTrue(result.isPresent());
    RestartPolicy policy = result.get();
    assertEquals(expectedMaxRetries, policy.getMaxRetries(), "Should use default when invalid");
    assertEquals(expectedInitialBackoff, policy.getInitialBackoffSeconds());
    assertEquals(expectedMaxBackoff, policy.getMaxBackoffSeconds());
  }

  private static Stream<Object[]> provideInvalidPropertyCases() {
    return Stream.of(
        new Object[] {"not-a-number", null, null, 5, 5, 300},
        new Object[] {null, "invalid", "also-invalid", 5, 5, 300});
  }

  @ParameterizedTest
  @CsvSource({"'   ', false", "'  always  ', true"})
  void testParseWhitespaceHandling(String policyValue, boolean shouldBePresent) {
    ApplicationDescriptor descriptor =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("restart.policy", policyValue)
            .build();

    Optional<RestartPolicy> result = parser.parse(descriptor);

    assertEquals(shouldBePresent, result.isPresent());
    if (shouldBePresent) {
      assertEquals(RestartPolicy.RestartCondition.ALWAYS, result.get().getCondition());
    }
  }
}
