# 🎪Test Environment

## Overview

`TestEnvironment` and `BaseTestEnvironment` are utility classes designed to help manage environment variables in your test environment. These classes provide a flexible way to:
- Read system environment variables
- Load variables from `.env` files
- Provide fallback default values
- Populate system properties

## Quick Start

The simplest way to use the test environment is through the `TestEnvironment` singleton object:

```kotlin
import me.kpavlov.finchly.TestEnvironment

class MyTest {
    @Test
    fun `test with environment variables`() {
        // Read an environment variable
        val homeDir = TestEnvironment["HOME"]
        
        // Read with a default fallback value
        val apiKey = TestEnvironment.get("API_KEY", defaultValue = "default-key")
    }
}
```

## Features

### Reading Environment Variables

The environment classes follow this precedence order when reading variables:
1. System environment variables (`System.getenv()`)
2. Values from `.env` file
3. Default values (if provided)
4. Define your TestEnvironment as singleton

```kotlin
// Different ways to read variables
val value1 = TestEnvironment["MY_VAR"]               // Returns null if not found
val value2 = TestEnvironment.get("MY_VAR", "default") // Returns "default" if not found
```

### Custom Environment Configuration

For more control over the environment configuration, you can create an instance of `BaseTestEnvironment`:

```kotlin
val testEnv = BaseTestEnvironment(
  populateSystemProperties = true,    // Whether to populate system properties
  dotEnvFileDir = "./config",         // Directory containing .env file
  dotEnvFileName = "test.env"         // Name of the .env file
)

class MyCustomTest {
    
    @Test
    fun `test with custom environment`() {
        val value = testEnv["DATABASE_URL"]
    }
}
```

### Environment File Support

The classes support loading variables from `.env` files. Here's an example `.env` file:

```properties
DATABASE_URL=jdbc:postgresql://localhost:5432/testdb
API_KEY=secret-test-key
CACHE_ENABLED=true
```

## Implementation Details

### Configuration Options

`BaseTestEnvironment` constructor parameters:
- `populateSystemProperties`: Boolean (default: `true`) - Whether to populate system properties with variables from `.env`
- `dotEnvFileDir`: String (default: `"./"`) - Directory containing the `.env` file
- `dotEnvFileName`: String (default: `".env"`) - Name of the environment file

### Error Handling

The environment loading is designed to be fault-tolerant:
- Missing `.env` files are ignored
- Malformed `.env` files are ignored
- Missing variables return `null` or the specified default value

## Best Practices

1. **Test-Specific Environments**
   ```kotlin
   class MyTest {
       private val testEnv = BaseTestEnvironment(
           dotEnvFileName = "test.env",
           dotEnvFileDir = "src/test/resources"
       )
   }
   ```

2. **Using Default Values**
   ```kotlin
   // Always provide meaningful defaults for optional configuration
   val timeout = testEnv.get("TIMEOUT_SECONDS", "30")
   ```

3. **Environment File Management**
  - Keep sensitive values in `.env` files
  - Add `.env` files to `.gitignore`
  - Provide `.env.example` files with template values

4. **Testing Different Configurations**
   ```kotlin
   class ConfigurationTest {
       private val prodEnv = BaseTestEnvironment(dotEnvFileName = "prod.env")
       private val stagingEnv = BaseTestEnvironment(dotEnvFileName = "staging.env")
       
       @Test
       fun `test configuration loading`() {
           assertThat(prodEnv["API_URL"]).isNotEqualTo(stagingEnv["API_URL"])
       }
   }
   ```

## Common Use Cases

### Integration Tests
```kotlin
class DatabaseIntegrationTest {
    private val testEnv = BaseTestEnvironment()
    
    private val dbUrl = testEnv.get("DATABASE_URL")
        ?: throw IllegalStateException("DATABASE_URL must be set")
        
    @Test
    fun `test database connection`() {
        // Use dbUrl for testing
    }
}
```

### Mock Services
```kotlin
class MockServiceTest {
    private val testEnv = BaseTestEnvironment()
    
    private val mockServiceUrl = testEnv.get("MOCK_SERVICE_URL", "http://localhost:8080")
    
    @Test
    fun `test external service integration`() {
        // Use mockServiceUrl for testing
    }
}
```

## Troubleshooting

### Common Issues

1. **Variables Not Loading**
  - Check the `.env` file location matches `dotEnvFileDir` and `dotEnvFileName`
  - Verify file permissions
  - Ensure the file is properly formatted

2. **System Properties Not Updating**
  - Verify `populateSystemProperties` is set to `true`
  - Check for conflicts with existing system properties

3. **Unexpected Default Values**
  - Remember the precedence order: System env > `.env` file > default value
  - Use logging or debugging to verify which source is being used

## Migration Guide

### From System.getenv()
```kotlin
// Old approach
val apiKey = System.getenv("API_KEY") ?: "default-key"

// New approach using TestEnvironment
val apiKey = TestEnvironment.get("API_KEY", "default-key")
```

### From Properties Files
```kotlin
// Old approach
val props = Properties().apply {
    load(FileInputStream("config.properties"))
}

// New approach using BaseTestEnvironment
val testEnv = BaseTestEnvironment(dotEnvFileName = "config.env")
```
