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

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback layout that masks sensitive data in log messages.
 *
 * <p>This layout extends PatternLayout to automatically redact sensitive information
 * such as API keys, passwords, tokens, and other credentials from log output.</p>
 *
 * <p>Example configuration in logback.xml:</p>
 * <pre>{@code
 * <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
 *   <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
 *     <layout class="org.flossware.platform.logging.MaskingPatternLayout">
 *       <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
 *       <maskPattern>"apiKey"\s*:\s*"([^"]+)"</maskPattern>
 *       <maskPattern>"password"\s*:\s*"([^"]+)"</maskPattern>
 *     </layout>
 *   </encoder>
 * </appender>
 * }</pre>
 *
 * <p><strong>Security Note:</strong> This class provides defense-in-depth for logging.
 * Applications should still avoid logging sensitive data in the first place.</p>
 *
 * @since 2.0
 */
public class MaskingPatternLayout extends PatternLayout {

    private static final String REDACTED = "***REDACTED***";
    private final List<Pattern> maskPatterns = new ArrayList<>();

    /**
     * Adds a regex pattern for masking sensitive data.
     *
     * <p>The pattern should contain at least one capture group. The first capture
     * group will be replaced with REDACTED. If no capture groups are present,
     * the entire match is replaced.</p>
     *
     * @param patternString the regex pattern to match sensitive data
     */
    public void addMaskPattern(String patternString) {
        if (patternString != null && !patternString.isEmpty()) {
            maskPatterns.add(Pattern.compile(patternString));
        }
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        String message = super.doLayout(event);
        return maskMessage(message);
    }

    /**
     * Masks sensitive data in the log message using configured patterns.
     *
     * @param message the original log message
     * @return the message with sensitive data redacted
     */
    private String maskMessage(String message) {
        if (message == null || maskPatterns.isEmpty()) {
            return message;
        }

        String masked = message;
        for (Pattern pattern : maskPatterns) {
            Matcher matcher = pattern.matcher(masked);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    // Replace first capture group with REDACTED
                    String replacement = matcher.group(0).replace(matcher.group(1), REDACTED);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                } else {
                    // No capture groups, replace entire match
                    matcher.appendReplacement(sb, REDACTED);
                }
            }
            matcher.appendTail(sb);
            masked = sb.toString();
        }

        return masked;
    }

    /**
     * Returns the configured mask patterns for testing purposes.
     *
     * @return list of compiled patterns
     */
    protected List<Pattern> getMaskPatterns() {
        return new ArrayList<>(maskPatterns);
    }
}
