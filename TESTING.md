# Testing Guide

This document describes the testing strategy and practices for the FlossWare Platform.

## Test Categories

Tests are categorized using JUnit 5 `@Tag` annotations for selective execution.

### Available Tags

| Tag | Description | Examples |
|-----|-------------|----------|
| `unit` | Fast, isolated unit tests | Method logic, validation, builders |
| `integration` | Tests with external dependencies | Database, network, file I/O |
| `security` | Security-focused tests | Input validation, auth, encryption |
| `performance` | Performance and load tests | Benchmarks, stress tests |
| `slow` | Long-running tests (>1 second) | Large data processing, retries |

### Running Tests by Category

#### Run All Tests (Default)

```bash
mvn test
```

#### Run Only Unit Tests (Fast)

```bash
mvn test -Dtest.groups=unit
```

#### Run Only Integration Tests

```bash
mvn test -Dtest.groups=integration
```

#### Run Security Tests

```bash
mvn test -Dtest.groups=security
```

#### Exclude Slow Tests

```bash
mvn test -Dtest.excludedGroups=slow
```

#### Multiple Tags (OR logic)

```bash
# Run unit OR security tests
mvn test -Dtest.groups="unit | security"
```

#### Multiple Tags (AND logic)

```bash
# Run tests that are BOTH unit AND security
mvn test -Dtest.groups="unit & security"
```

#### Complex Expressions

```bash
# Run integration tests but exclude slow ones
mvn test -Dtest.groups=integration -Dtest.excludedGroups=slow
```

## Writing Tests

### Basic Test Structure

```java
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MyServiceTest {
  
  @Test
  void shouldCalculateCorrectly() {
    // Arrange
    MyService service = new MyService();
    
    // Act
    int result = service.calculate(5, 3);
    
    // Assert
    assertEquals(8, result);
  }
}
```

### Test with Multiple Tags

```java
@Tag("integration")
@Tag("slow")
class DatabaseIntegrationTest {
  
  @Test
  void shouldConnectToDatabase() {
    // Test implementation
  }
}
```

### Security Test Example

```java
@Tag("unit")
@Tag("security")
class InputValidationTest {
  
  @Test
  void shouldRejectPathTraversal() {
    assertThrows(SecurityException.class, () -> {
      validator.validate("../../../etc/passwd");
    });
  }
}
```

## Test Naming Conventions

### Method Names

Use descriptive names following this pattern:

```java
// Pattern: should[ExpectedBehavior]When[StateUnderTest]

@Test
void shouldThrowExceptionWhenInputIsNull() { }

@Test
void shouldReturnEmptyListWhenDatabaseIsEmpty() { }

@Test
void shouldCalculateDiscountWhenUserIsPremium() { }
```

### Class Names

```java
// Pattern: [ClassName]Test

class UserServiceTest { }
class ApplicationManagerTest { }
class SecurityValidatorTest { }
```

## Coverage Requirements

- **Minimum**: 60% overall coverage (enforced by JaCoCo)
- **Target**: 80%+ for critical modules
- **Security Code**: 90%+ coverage required

### Check Coverage

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Coverage by Module

```bash
# Run coverage for specific module
cd platform-core
mvn clean test jacoco:report
```

## Test Data

### Temporary Files

Use `@TempDir` for temporary file/directory tests:

```java
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class FileProcessorTest {
  
  @TempDir
  Path tempDir;
  
  @Test
  void shouldProcessFile() {
    Path testFile = tempDir.resolve("test.txt");
    Files.writeString(testFile, "content");
    // Test with testFile
  }
}
```

### Test Fixtures

```java
@BeforeEach
void setUp() {
  // Initialize test data
  testUser = new User("user123", "Test User");
}

@AfterEach
void tearDown() {
  // Clean up resources
}
```

## Assertions

### Common Assertions

```java
// Equality
assertEquals(expected, actual);
assertNotEquals(unexpected, actual);

// Boolean
assertTrue(condition);
assertFalse(condition);

// Null checks
assertNull(value);
assertNotNull(value);

// Exceptions
assertThrows(IllegalArgumentException.class, () -> {
  service.methodThatThrows();
});

// Timeout
assertTimeout(Duration.ofSeconds(1), () -> {
  // Fast operation
});

// Collections
assertIterableEquals(expectedList, actualList);
```

### Custom Messages

```java
assertEquals(expected, actual, 
  "User ID should match the created user");
```

## Parameterized Tests

Test multiple inputs efficiently:

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
class ParameterizedExampleTest {
  
  @ParameterizedTest
  @ValueSource(strings = {"user1", "user2", "user3"})
  void shouldValidateUsername(String username) {
    assertTrue(validator.isValid(username));
  }
  
  @ParameterizedTest
  @CsvSource({
    "1, 2, 3",
    "5, 5, 10",
    "10, -5, 5"
  })
  void shouldAddNumbers(int a, int b, int expected) {
    assertEquals(expected, calculator.add(a, b));
  }
}
```

## Mock Objects

Use Mockito for mocking dependencies:

```java
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceWithDependenciesTest {
  
  @Mock
  private DatabaseRepository repository;
  
  @Test
  void shouldCallRepository() {
    when(repository.findById("123"))
      .thenReturn(Optional.of(testUser));
    
    User result = service.getUser("123");
    
    verify(repository).findById("123");
    assertEquals(testUser, result);
  }
}
```

## CI/CD Integration

### Default CI Pipeline

All tests run by default:

```yaml
- name: Run tests
  run: mvn clean verify
```

### Fast Feedback (Unit Tests Only)

```yaml
- name: Quick test
  run: mvn test -Dtest.groups=unit
```

### Full Test Suite

```yaml
- name: Unit tests
  run: mvn test -Dtest.groups=unit

- name: Integration tests
  run: mvn test -Dtest.groups=integration

- name: Security tests
  run: mvn test -Dtest.groups=security
```

### Nightly Build (All Tests)

```yaml
- name: Full test suite
  run: mvn clean verify
```

## Best Practices

### 1. Independent Tests

Each test should be independent and isolated:

```java
// Good - self-contained
@Test
void shouldCalculate() {
  Calculator calc = new Calculator();
  assertEquals(4, calc.add(2, 2));
}

// Bad - depends on execution order
static int counter = 0;

@Test
void firstTest() {
  counter = 5;
}

@Test
void secondTest() {
  assertEquals(5, counter); // Fails if run alone
}
```

### 2. One Assertion Concept Per Test

```java
// Good - tests one thing
@Test
void shouldValidateEmail() {
  assertTrue(validator.isValidEmail("user@example.com"));
}

@Test
void shouldRejectInvalidEmail() {
  assertFalse(validator.isValidEmail("invalid"));
}

// Avoid - tests multiple things
@Test
void emailValidation() {
  assertTrue(validator.isValidEmail("user@example.com"));
  assertFalse(validator.isValidEmail("invalid"));
  assertThrows(Exception.class, () -> validator.isValidEmail(null));
}
```

### 3. Clear Test Names

```java
// Good - describes behavior
@Test
void shouldReturnNullWhenUserNotFound() { }

// Bad - unclear
@Test
void test1() { }
```

### 4. Arrange-Act-Assert Pattern

```java
@Test
void shouldCalculateDiscount() {
  // Arrange
  Order order = new Order(100.00);
  DiscountCalculator calculator = new DiscountCalculator();
  
  // Act
  double discount = calculator.calculate(order);
  
  // Assert
  assertEquals(10.00, discount);
}
```

### 5. Test Edge Cases

```java
@Test
void shouldHandleEmptyInput() { }

@Test
void shouldHandleNullInput() { }

@Test
void shouldHandleMaximumValue() { }

@Test
void shouldHandleNegativeValue() { }
```

## Troubleshooting

### Tests Pass Locally But Fail in CI

- Check for timezone dependencies
- Check for file path separators (Windows vs Linux)
- Verify test isolation (no shared state)

### Flaky Tests

- Use `@Timeout` to catch slow tests
- Avoid `Thread.sleep()` - use proper synchronization
- Mock time-dependent operations

### Coverage Not Meeting Threshold

```bash
# Identify uncovered code
mvn clean test jacoco:report
open target/site/jacoco/index.html

# Add tests for red/yellow highlighted code
```

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
