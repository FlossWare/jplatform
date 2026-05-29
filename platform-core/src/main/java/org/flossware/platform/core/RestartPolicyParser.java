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

import java.util.Map;
import java.util.Optional;

import org.flossware.platform.api.ApplicationDescriptor;
import org.flossware.platform.api.RestartPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses restart policies from application descriptor properties.
 *
 * <p>Supported properties:
 *
 * <ul>
 *   <li>restart.policy - "always", "on-failure", "never" (default: "never")
 *   <li>restart.maxRetries - max restart attempts (default: Integer.MAX_VALUE for always, 5 for
 *       on-failure)
 *   <li>restart.initialBackoff - initial delay in seconds (default: 5)
 *   <li>restart.maxBackoff - maximum delay in seconds (default: 300)
 * </ul>
 *
 * <p>Example descriptor properties:
 *
 * <pre>{@code
 * restart.policy=on-failure
 * restart.maxRetries=10
 * restart.initialBackoff=10
 * restart.maxBackoff=600
 * }</pre>
 *
 * @since 2.3
 */
class RestartPolicyParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestartPolicyParser.class);

  private static final String PROP_POLICY = "restart.policy";
  private static final String PROP_MAX_RETRIES = "restart.maxRetries";
  private static final String PROP_INITIAL_BACKOFF = "restart.initialBackoff";
  private static final String PROP_MAX_BACKOFF = "restart.maxBackoff";

  /**
   * Parses a restart policy from application descriptor properties.
   *
   * @param descriptor the application descriptor
   * @return the parsed restart policy, or empty if no restart policy configured
   */
  Optional<RestartPolicy> parse(ApplicationDescriptor descriptor) {
    Map<String, String> props = descriptor.getProperties();

    String policyStr = props.get(PROP_POLICY);
    if (policyStr == null || policyStr.trim().isEmpty()) {
      // No restart policy configured
      return Optional.empty();
    }

    try {
      switch (policyStr.trim().toLowerCase()) {
        case "never":
          return Optional.of(RestartPolicy.never());

        case "always":
          return Optional.of(RestartPolicy.always());

        case "on-failure":
          int maxRetries = parseIntProperty(props, PROP_MAX_RETRIES, 5);
          long initialBackoff = parseLongProperty(props, PROP_INITIAL_BACKOFF, 5);
          long maxBackoff = parseLongProperty(props, PROP_MAX_BACKOFF, 300);

          return Optional.of(RestartPolicy.onFailure(maxRetries, initialBackoff, maxBackoff));

        default:
          LOGGER.warn(
              "[{}] Unknown restart policy '{}', using 'never'",
              descriptor.getApplicationId(),
              policyStr);
          return Optional.of(RestartPolicy.never());
      }
    } catch (Exception e) {
      LOGGER.error(
          "[{}] Failed to parse restart policy, using 'never'", descriptor.getApplicationId(), e);
      return Optional.of(RestartPolicy.never());
    }
  }

  private int parseIntProperty(Map<String, String> props, String key, int defaultValue) {
    String value = props.get(key);
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      LOGGER.warn(
          "Invalid integer value for '{}': '{}', using default {}", key, value, defaultValue);
      return defaultValue;
    }
  }

  private long parseLongProperty(Map<String, String> props, String key, long defaultValue) {
    String value = props.get(key);
    if (value == null || value.trim().isEmpty()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      LOGGER.warn("Invalid long value for '{}': '{}', using default {}", key, value, defaultValue);
      return defaultValue;
    }
  }
}
