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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for HealthCheckConfigParser. */
@Tag("unit")
class HealthCheckConfigParserTest {

  private HealthCheckConfigParser parser;

  @BeforeEach
  void setUp() {
    parser = new HealthCheckConfigParser();
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", ""})
  void testParseNotEnabled(String enabledValue) {
    ApplicationDescriptor.Builder builder =
        ApplicationDescriptor.builder().applicationId("test-app").mainClass("com.example.App");

    if (!enabledValue.isEmpty()) {
      builder.property("healthcheck.enabled", enabledValue);
    }

    Optional<HealthChecker.HealthCheckConfig> result = parser.parse(builder.build());

    assertFalse(result.isPresent(), "Should return empty when health checks not enabled");
  }

  @ParameterizedTest
  @MethodSource("provideHealthCheckTypeTestCases")
  void testParseHealthCheckTypes(
      String type,
      HealthChecker.HealthCheckType expectedType,
      String httpUrl,
      String tcpHost,
      String tcpPort) {
    ApplicationDescriptor.Builder builder =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("healthcheck.enabled", "true")
            .property("healthcheck.type", type);

    if (httpUrl != null) {
      builder.property("healthcheck.http.url", httpUrl);
    }
    if (tcpHost != null) {
      builder.property("healthcheck.tcp.host", tcpHost);
    }
    if (tcpPort != null) {
      builder.property("healthcheck.tcp.port", tcpPort);
    }

    Optional<HealthChecker.HealthCheckConfig> result = parser.parse(builder.build());

    assertTrue(result.isPresent());
    HealthChecker.HealthCheckConfig config = result.get();
    assertEquals(expectedType, config.getType());
    if (httpUrl != null) {
      assertEquals(httpUrl, config.getHttpUrl());
    }
    if (tcpHost != null) {
      assertEquals(tcpHost, config.getTcpHost());
    }
    if (tcpPort != null) {
      assertEquals(Integer.parseInt(tcpPort), config.getTcpPort());
    }
  }

  private static Stream<Object[]> provideHealthCheckTypeTestCases() {
    return Stream.of(
        new Object[] {
          "http", HealthChecker.HealthCheckType.HTTP, "http://localhost:8080/health", null, null
        },
        new Object[] {"tcp", HealthChecker.HealthCheckType.TCP, null, "localhost", "8080"});
  }

  @ParameterizedTest
  @CsvSource({"HTTP, HTTP", "unknown-type, APPLICATION"})
  void testParseTypeEdgeCases(String typeValue, HealthChecker.HealthCheckType expectedType) {
    ApplicationDescriptor.Builder builder =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("healthcheck.enabled", "true")
            .property("healthcheck.type", typeValue);

    if (expectedType == HealthChecker.HealthCheckType.HTTP) {
      builder.property("healthcheck.http.url", "http://localhost:8080/health");
    }

    Optional<HealthChecker.HealthCheckConfig> result = parser.parse(builder.build());

    assertTrue(result.isPresent());
    assertEquals(expectedType, result.get().getType());
  }

  @ParameterizedTest
  @CsvSource({"not-a-number, '', '', 30", "'', invalid, localhost, 0"})
  void testParseInvalidPropertiesUseDefaults(
      String interval, String tcpPort, String tcpHost, int expectedValue) {
    ApplicationDescriptor.Builder builder =
        ApplicationDescriptor.builder()
            .applicationId("test-app")
            .mainClass("com.example.App")
            .property("healthcheck.enabled", "true");

    if (!interval.isEmpty()) {
      builder.property("healthcheck.interval", interval);
    }
    if (!tcpPort.isEmpty()) {
      builder
          .property("healthcheck.type", "tcp")
          .property("healthcheck.tcp.host", tcpHost)
          .property("healthcheck.tcp.port", tcpPort);
    }

    Optional<HealthChecker.HealthCheckConfig> result = parser.parse(builder.build());

    assertTrue(result.isPresent());
    if (!interval.isEmpty()) {
      assertEquals(
          expectedValue, result.get().getIntervalSeconds(), "Should use default when invalid");
    } else {
      assertEquals(expectedValue, result.get().getTcpPort());
    }
  }
}
