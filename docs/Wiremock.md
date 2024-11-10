# 🎭 Finchly Wiremock

## Overview

`BaseWiremock` is an abstract base class that simplifies the management of WireMock servers for integration testing. It provides a wrapper around `WireMockServer` with convenient initialization and management methods, making it easier to mock HTTP services in your tests.

## Quick Start

Here's a basic example of how to create and use a WireMock server in your tests:

```kotlin
class MyApiTest {
    // Create a WireMock instance with custom stubs
    private val mockServer = object : BaseWiremock({}) {
        fun stubGetUserProfile() {
            mock.stubFor(
                WireMock.get("/api/profile")
                    .willReturn(WireMock.okJson("""{"name": "John Doe"}"""))
            )
        }
    }

    @Test
    fun `test API interaction`() {
        // Setup the stub
        mockServer.stubGetUserProfile()

        // Make your API call
        val response = yourApiClient.getUserProfile()

        // Verify the response
        assertThat(response.name).isEqualTo("John Doe")
    }
}
```

## Features

### Server Initialization

`BaseWiremock` offers several ways to initialize the WireMock server:

1. **Using Configuration Block**
```kotlin
private val mockServer = object : BaseWiremock({
    it.dynamicPort()           // Use random available port
    it.disableRequestJournal() // Disable request journal if needed
}) {
    // Your stub methods here
}
```

2. **Using WireMockConfiguration**
```kotlin
private val mockServer = object : BaseWiremock(
    WireMockConfiguration.options()
        .port(8080)
        .disableRequestJournal()
) {
    // Your stub methods here
}
```

3. **Using Existing WireMockServer**
```kotlin
private val mockServer = object : BaseWiremock(
    WireMockServer(WireMockConfiguration.options().port(8080))
) {
    // Your stub methods here
}
```

### Core Features

1. **Dynamic Port Allocation**

```kotlin
// Get the port number assigned to the server
val port = mockServer.port()
```

2. **Stub Management**
```kotlin
// Reset specific stub
mockServer.resetStub(stubMapping)

// Reset all stubs
mockServer.resetAllStubs()
```

3. **Request Verification**
```kotlin
// Verify no unmatched requests
mockServer.verifyNoUnmatchedRequests()
```

## Best Practices

### 1. Organize Stubs in Extension Functions

```kotlin
class MyApiTest {
    private val mockServer = object : BaseWiremock({}) {
        // Group related stubs together
        fun stubUserEndpoints() {
            stubGetUserProfile()
            stubUpdateUserProfile()
            stubDeleteUserProfile()
        }

        private fun stubGetUserProfile() {
            mock.stubFor(
                WireMock.get("/api/profile")
                    .willReturn(WireMock.okJson("""{"name": "John"}"""))
            )
        }

        private fun stubUpdateUserProfile() {
            mock.stubFor(
                WireMock.put("/api/profile")
                    .willReturn(WireMock.ok())
            )
        }

        private fun stubDeleteUserProfile() {
            mock.stubFor(
                WireMock.delete("/api/profile")
                    .willReturn(WireMock.ok())
            )
        }
    }
}
```

### 2. Use Request Matching

```kotlin
private val mockServer = object : BaseWiremock({}) {
    fun stubWithRequestMatching() {
        mock.stubFor(
            WireMock.post("/api/data")
                .withHeader("Content-Type", WireMock.equalTo("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.type", WireMock.equalTo("test")))
                .willReturn(WireMock.ok())
        )
    }
}
```

### 3. Handle Different Response Types

```kotlin
private val mockServer = object : BaseWiremock({}) {
    fun stubVariousResponses() {
        // JSON Response
        mock.stubFor(
            WireMock.get("/api/data")
                .willReturn(WireMock.okJson("""{"status": "success"}"""))
        )

        // Error Response
        mock.stubFor(
            WireMock.get("/api/error")
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        // Response with Headers
        mock.stubFor(
            WireMock.get("/api/protected")
                .willReturn(
                    WireMock.ok()
                        .withHeader("Authorization", "Bearer token")
                )
        )
    }
}
```

## Common Testing Patterns

### 1. Integration Test Setup

```kotlin
class IntegrationTest {
    private val mockServer = object : BaseWiremock({
        it.dynamicPort()
    }) {
        fun stubExternalService() {
            mock.stubFor(
                WireMock.get("/external/api")
                    .willReturn(WireMock.ok("Success"))
            )
        }
    }

    private val client = HttpClient.newHttpClient()

    @Test
    fun `test external service integration`() {
        // Setup
        mockServer.stubExternalService()
        
        // Execute
        val request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:${mockServer.port()}/external/api"))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        // Verify
        assertThat(response.body()).isEqualTo("Success")
    }
}
```

### 2. Testing Error Scenarios

```kotlin
class ErrorHandlingTest {
    private val mockServer = object : BaseWiremock({}) {
        fun stubServiceUnavailable() {
            mock.stubFor(
                WireMock.get("/api/service")
                    .willReturn(
                        WireMock.serviceUnavailable()
                            .withBody("Service Unavailable")
                    )
            )
        }
    }

    @Test
    fun `should handle service unavailable`() {
        mockServer.stubServiceUnavailable()
        // Test your error handling logic
    }
}
```

## Troubleshooting

### Common Issues

1. **Unmatched Requests**
```kotlin
@Test
fun `test with verification`() {
    try {
        // Your test code
        mockServer.verifyNoUnmatchedRequests()
    } catch (e: VerificationException) {
        println("Unmatched requests found: ${e.message}")
        throw e
    }
}
```

2. **Port Conflicts**
```kotlin
// Always use dynamic ports in tests to avoid conflicts
private val mockServer = object : BaseWiremock({
    it.dynamicPort()
}) {
    // Your stubs
}
```

3. **Memory Usage**
```kotlin
// Disable request journal for performance
private val mockServer = object : BaseWiremock({
    it.disableRequestJournal()
}) {
    // Your stubs
}
```

## Configuration Options

Key configuration options available:

```kotlin
private val mockServer = object : BaseWiremock({
    it.dynamicPort()                     // Use random port
    it.port(8080)                        // Use specific port
    it.disableRequestJournal()           // Disable request recording
    it.maxRequestJournalEntries(100)     // Limit journal entries
    it.extensionScanningEnabled(true)    // Enable extensions
}) {
    // Your stubs
}
```

## Best Practices for Clean Tests

### 1. Use Singleton Mock Objects

Instead of creating mock instances per test class, define them as singleton objects to improve performance and resource usage:

```kotlin
// Define mock as a singleton object
object UserServiceMock : BaseWiremock({
    it.dynamicPort()
}) {
    fun stubGetUserProfile(userId: String) {
        mock.stubFor(
            WireMock.get("/api/users/$userId")
                .willReturn(WireMock.okJson("""{"id": "$userId", "name": "John Doe"}"""))
        )
    }
    
    fun stubUpdateUserProfile(userId: String) {
        mock.stubFor(
            WireMock.put("/api/users/$userId")
                .willReturn(WireMock.ok())
        )
    }
}

// Usage in tests
class UserServiceTest {
    @Test
    fun `test get user profile`() {
        val userId = "user123"
        UserServiceMock.stubGetUserProfile(userId)
        
        val response = yourApiClient.getUserProfile(userId)
        assertThat(response.id).isEqualTo(userId)
    }
}
```

### 2. Use Descriptive Stub Methods
```kotlin
fun stubSuccessfulPayment() { /* ... */ }
fun stubFailedPayment() { /* ... */ }
fun stubPaymentTimeout() { /* ... */ }
```

### 3. Organize Stubs by Feature
```kotlin
private val mockServer = object : BaseWiremock({}) {
    // Authentication stubs
    fun stubAuthEndpoints() { /* ... */ }
    
    // User management stubs
    fun stubUserEndpoints() { /* ... */ }
    
    // Payment stubs
    fun stubPaymentEndpoints() { /* ... */ }
}
```

### 4. Support Parallel Test Execution

Design mock methods to accept unique identifiers to allow parallel test execution:

```kotlin
object PaymentServiceMock : BaseWiremock({}) {
    // Bad: Fixed endpoint that may conflict in parallel tests
    fun stubPaymentProcessing() {
        mock.stubFor(
            WireMock.post("/api/payments")
                .willReturn(WireMock.ok())
        )
    }
    
    // Good: Accepts unique transaction ID for parallel execution
    fun stubPaymentProcessing(transactionId: String) {
        mock.stubFor(
            WireMock.post("/api/payments/$transactionId")
                .willReturn(WireMock.ok())
        )
    }
}

class PaymentTests {
    @Test
    fun `test successful payment`() {
        val transactionId = UUID.randomUUID().toString()
        PaymentServiceMock.stubPaymentProcessing(transactionId)
        
        val result = paymentService.processPayment(transactionId)
        assertThat(result.status).isEqualTo("SUCCESS")
    }
}
```

### 5. Combine Singleton Mocks with Dynamic Values

```kotlin
object OrderServiceMock : BaseWiremock({}) {
    // Accept unique identifiers for parallel test execution
    fun stubOrderFlow(orderId: String, userId: String) {
        stubCreateOrder(orderId, userId)
        stubGetOrderStatus(orderId)
        stubUpdateOrder(orderId)
    }
    
    private fun stubCreateOrder(orderId: String, userId: String) {
        mock.stubFor(
            WireMock.post("/api/orders")
                .withRequestBody(WireMock.matchingJsonPath("$.userId", WireMock.equalTo(userId)))
                .willReturn(
                    WireMock.okJson("""{"orderId": "$orderId", "status": "CREATED"}""")
                )
        )
    }
    
    private fun stubGetOrderStatus(orderId: String) {
        mock.stubFor(
            WireMock.get("/api/orders/$orderId/status")
                .willReturn(WireMock.okJson("""{"status": "IN_PROGRESS"}"""))
        )
    }
    
    private fun stubUpdateOrder(orderId: String) {
        mock.stubFor(
            WireMock.put("/api/orders/$orderId")
                .willReturn(WireMock.ok())
        )
    }
}

// Usage in parallel tests
class OrderProcessingTests {
    @Test
    fun `test order creation and processing`() {
        val orderId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        
        OrderServiceMock.stubOrderFlow(orderId, userId)
        
        val result = orderService.createAndProcessOrder(userId)
        assertThat(result.orderId).isEqualTo(orderId)
    }
}
```

### 6. Use Test-Specific Data With Singleton Mocks

```kotlin
object UserProfileMock : BaseWiremock({}) {
    fun stubUserProfile(
        userId: String,
        userData: UserData = UserData(userId, "John Doe", "john@example.com")
    ) {
        mock.stubFor(
            WireMock.get("/api/users/$userId")
                .willReturn(
                    WireMock.okJson(
                        """
                        {
                            "id": "${userData.id}",
                            "name": "${userData.name}",
                            "email": "${userData.email}"
                        }
                        """.trimIndent()
                    )
                )
        )
    }
    
    data class UserData(
        val id: String,
        val name: String,
        val email: String
    )
}

class UserTests {
    @Test
    fun `test custom user profile`() {
        val userId = UUID.randomUUID().toString()
        val customData = UserProfileMock.UserData(
            id = userId,
            name = "Jane Doe",
            email = "jane@example.com"
        )
        
        UserProfileMock.stubUserProfile(userId, customData)
        
        val result = userService.getUserProfile(userId)
        assertThat(result.name).isEqualTo("Jane Doe")
    }
}
```

### 7. Manage Multiple Singleton Mocks

When your application interacts with multiple services, organize related mocks together:

```kotlin
// Group related mocks in an object
object TestMocks {
    val userService = UserServiceMock
    val paymentService = PaymentServiceMock
    val orderService = OrderServiceMock
  
    
    // Setup common test scenarios
    fun stubSuccessfulOrderFlow(userId: String, orderId: String) {
        userService.stubGetUserProfile(userId)
        orderService.stubOrderFlow(orderId, userId)
        paymentService.stubPaymentProcessing(orderId)
    }
}

class IntegrationTests {
 
    @Test
    fun `test complete order flow`() {
        val userId = UUID.randomUUID().toString()
        val orderId = UUID.randomUUID().toString()
        
        TestMocks.stubSuccessfulOrderFlow(userId, orderId)
        
        val result = orderProcessingService.processOrder(userId)
        assertThat(result.status).isEqualTo("SUCCESS")
    }
}
```

### 8. Verify Unmatched Requests
Always verify there are no unmatched requests after each test to:

Detect missing or incorrectly configured stubs
Identify unexpected service behavior
Ensure all expected requests were properly mocked
Catch integration issues early
These best practices ensure that:
- Tests can run in parallel without conflicts
- Resources are efficiently managed through singleton instances
- Mock definitions are reusable across test classes
- Test data can be customized while maintaining consistent behavior
- Related mocks can be organized and managed together
- Common test scenarios can be easily set up

#### Basic JUnit 5 Implementation

```kotlin
class ApiTest {
private val mockServer = MyServiceMock

    @AfterEach
    fun verifyMocks() {
        mockServer.verifyNoUnmatchedRequests()
    }
    
    @Test
    fun `test api call`() {
        mockServer.stubEndpoint("data")
        service.getData() // If this makes an unexpected call, verifyMocks() will fail
    }
}
```

#### Abstract Base Test Class

```kotlin
abstract class BaseApiTest {
    @AfterEach
    fun verifyAllMocks() {
        // Verify all singleton mocks
        TestMocks.userService.verifyNoUnmatchedRequests()
        TestMocks.orderService.verifyNoUnmatchedRequests()
        TestMocks.paymentService.verifyNoUnmatchedRequests()
    }
}

class UserServiceTest : BaseApiTest() {
    @Test
    fun `test user profile`() {
        val userId = UUID.randomUUID().toString()
        TestMocks.userService.stubGetUserProfile(userId)
        
        userService.getUserProfile(userId)
        // verifyAllMocks() will run automatically after the test
    }
}
```

