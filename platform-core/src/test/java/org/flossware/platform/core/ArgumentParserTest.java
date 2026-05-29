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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Comprehensive unit tests for ArgumentParser. Tests command-line argument parsing with various
 * quote scenarios.
 */
@Tag("unit")
class ArgumentParserTest {

  @ParameterizedTest
  @ValueSource(strings = {"", "   \t  \n  "})
  void testParseArgumentsEmptyInput(String input) {
    List<String> result = ArgumentParser.parseArguments(input);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testParseArgumentsNull() {
    List<String> result = ArgumentParser.parseArguments(null);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
    --config file.json --port 8080 | file.json
    --config "/path with spaces/config.json" --port 8080 | /path with spaces/config.json
    --config '/path with spaces/config.json' --port 8080 | /path with spaces/config.json
    """)
  void testParseArgumentsConfigPort(String input, String expectedPath) {
    List<String> result = ArgumentParser.parseArguments(input);

    assertEquals(4, result.size());
    assertEquals("--config", result.get(0));
    assertEquals(expectedPath, result.get(1));
    assertEquals("--port", result.get(2));
    assertEquals("8080", result.get(3));
  }

  @ParameterizedTest
  @MethodSource("provideAllArgumentTestCases")
  void testParseArgumentsAllCases(String input, String[] expectedOutputs) {
    List<String> result = ArgumentParser.parseArguments(input);

    assertEquals(expectedOutputs.length, result.size());
    for (int i = 0; i < expectedOutputs.length; i++) {
      assertEquals(expectedOutputs[i], result.get(i));
    }
  }

  private static Stream<Object[]> provideAllArgumentTestCases() {
    return Stream.of(
        new Object[] {
          "--message \"He said \\\"hello\\\"\"", new String[] {"--message", "He said \"hello\""}
        },
        new Object[] {"--message 'It\\'s working'", new String[] {"--message", "It's working"}},
        new Object[] {
          "--path \"C:\\\\Windows\\\\System32\"", new String[] {"--path", "C:\\Windows\\System32"}
        },
        new Object[] {"--message \"Line1\\nLine2\"", new String[] {"--message", "Line1\nLine2"}},
        new Object[] {"--message \"Col1\\tCol2\"", new String[] {"--message", "Col1\tCol2"}},
        new Object[] {"--message \"Before\\rAfter\"", new String[] {"--message", "Before\rAfter"}},
        new Object[] {"--message \"test\\xvalue\"", new String[] {"--message", "test\\xvalue"}},
        new Object[] {"--name \"\" --value ''", new String[] {"--name", "", "--value", ""}},
        new Object[] {
          "\"first arg\" 'second arg' \"third arg\"",
          new String[] {"first arg", "second arg", "third arg"}
        },
        new Object[] {
          "--json \"{\\\"key\\\":\\\"value\\\"}\"", new String[] {"--json", "{\"key\":\"value\"}"}
        },
        new Object[] {
          "--text \"Line1\\nLine2\\tCol\\\\Backslash\\\"Quote\"",
          new String[] {"--text", "Line1\nLine2\tCol\\Backslash\"Quote"}
        },
        new Object[] {"--path \"C:\\\\Users\\\\\"", new String[] {"--path", "C:\\Users\\"}});
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
    standalone | standalone
    "single quoted token with spaces" | single quoted token with spaces
    """)
  void testParseArgumentsSingleToken(String input, String expected) {
    List<String> result = ArgumentParser.parseArguments(input);

    assertEquals(1, result.size());
    assertEquals(expected, result.get(0));
  }

  @ParameterizedTest
  @MethodSource("provideVariousParsingScenarios")
  void testParseArgumentsVariousScenarios(String input, String[] expectedOutputs) {
    List<String> result = ArgumentParser.parseArguments(input);

    assertEquals(expectedOutputs.length, result.size());
    for (int i = 0; i < expectedOutputs.length; i++) {
      assertEquals(expectedOutputs[i], result.get(i));
    }
  }

  private static Stream<Object[]> provideVariousParsingScenarios() {
    return Stream.of(
        new Object[] {
          "--file=/path/to/file.txt --port=8080",
          new String[] {"--file=/path/to/file.txt", "--port=8080"}
        },
        new Object[] {
          "--config    file.json     --port   8080",
          new String[] {"--config", "file.json", "--port", "8080"}
        },
        new Object[] {
          "\"double quoted\" 'single quoted' unquoted",
          new String[] {"double quoted", "single quoted", "unquoted"}
        });
  }
}
