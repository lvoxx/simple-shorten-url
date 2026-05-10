# Spring Retry

Spring Retry provides declarative and imperative retry support for Spring applications. It offers an abstraction around retrying failed operations with emphasis on declarative control of the process and policy-based behavior that is easy to extend and customize. The library is used in Spring Batch, Spring Integration, and other Spring projects.

This project supports both annotation-driven declarative retry using `@Retryable` and programmatic imperative retry using `RetryTemplate`. Key features include configurable retry policies, multiple backoff strategies (fixed, exponential, random), circuit breaker pattern support, recovery callbacks for fallback behavior, and Micrometer metrics integration for monitoring retry operations.

## @EnableRetry - Enable Declarative Retry

Enables `@Retryable` annotations in Spring beans. When declared on any `@Configuration` class, beans with retryable methods will be proxied and retry handled according to annotation metadata.

```java
@Configuration
@EnableRetry
public class AppConfig {
    // Enables retry support for all @Retryable methods in the application context
}

// With CGLIB proxy mode for class-based proxies
@Configuration
@EnableRetry(proxyTargetClass = true)
public class AppConfig {
    // Uses CGLIB proxies instead of JDK dynamic proxies
}

// Control advice ordering (runs before @Transactional by default)
@Configuration
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE - 1)
public class AppConfig {
    // Custom ordering to ensure retry advice runs at specific point
}
```

## @Retryable - Declarative Method Retry

Marks a method for automatic retry on failure. Supports configurable exception types, max attempts, backoff strategies, and recovery methods.

```java
@Service
public class RemoteService {

    // Basic retry - retries up to 3 times (default) on any exception
    @Retryable
    public String fetchData() {
        return callRemoteApi();
    }

    // Retry only for specific exceptions
    @Retryable(retryFor = {RemoteAccessException.class, TimeoutException.class})
    public String fetchWithSpecificRetry() {
        return callRemoteApi();
    }

    // Exclude certain exceptions from retry
    @Retryable(retryFor = Exception.class, noRetryFor = IllegalArgumentException.class)
    public String fetchWithExclusions(String param) {
        if (param == null) throw new IllegalArgumentException("No retry for this");
        return callRemoteApi();
    }

    // Custom max attempts with exponential backoff
    @Retryable(
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000)
    )
    public String fetchWithBackoff() {
        return callRemoteApi();
    }

    // Fixed backoff between retries
    @Retryable(
        maxAttempts = 4,
        backoff = @Backoff(delay = 2000)  // 2 second fixed delay
    )
    public String fetchWithFixedDelay() {
        return callRemoteApi();
    }

    // Random backoff with jitter
    @Retryable(
        maxAttempts = 5,
        backoff = @Backoff(delay = 500, maxDelay = 3000, random = true)
    )
    public String fetchWithRandomBackoff() {
        return callRemoteApi();
    }

    // Expression-based configuration
    @Retryable(
        maxAttemptsExpression = "#{@retryConfig.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "#{@retryConfig.initialDelay}",
            maxDelayExpression = "#{@retryConfig.maxDelay}",
            multiplierExpression = "#{@retryConfig.multiplier}"
        )
    )
    public String fetchWithExpressionConfig() {
        return callRemoteApi();
    }

    // Conditional retry based on exception message
    @Retryable(exceptionExpression = "message.contains('temporary')")
    public String fetchWithConditionalRetry() {
        return callRemoteApi();
    }

    // Stateful retry - maintains state across invocations
    @Retryable(stateful = true, retryFor = RemoteAccessException.class)
    public String fetchStateful(String key) {
        return callRemoteApi();
    }

    // Specify explicit recovery method
    @Retryable(recover = "fetchRecover", retryFor = RemoteAccessException.class)
    public String fetchWithNamedRecovery() {
        return callRemoteApi();
    }

    @Recover
    public String fetchRecover(RemoteAccessException e) {
        return "fallback-value";
    }
}
```

## @Recover - Recovery Method for Failed Retries

Defines a recovery method invoked when all retry attempts are exhausted. The method must be in the same class, have a compatible return type, and optionally accept the exception and original arguments.

```java
@Service
public class PaymentService {

    @Retryable(retryFor = PaymentException.class, maxAttempts = 3)
    public PaymentResult processPayment(String orderId, BigDecimal amount) {
        return paymentGateway.process(orderId, amount);
    }

    // Recovery method with exception and original arguments
    @Recover
    public PaymentResult recoverPayment(PaymentException e, String orderId, BigDecimal amount) {
        log.error("Payment failed for order {}: {}", orderId, e.getMessage());
        return PaymentResult.pending(orderId, "Payment queued for manual review");
    }

    // Multiple retryable methods with different recovery handlers
    @Retryable(recover = "recoverRefund", retryFor = RefundException.class)
    public RefundResult processRefund(String transactionId) {
        return paymentGateway.refund(transactionId);
    }

    @Recover
    public RefundResult recoverRefund(RefundException e, String transactionId) {
        return RefundResult.queued(transactionId);
    }

    // Recovery with generic return type matching
    @Retryable(retryFor = RemoteAccessException.class)
    public List<Order> fetchOrders(String customerId) {
        return orderService.getOrders(customerId);
    }

    @Recover
    public List<Order> recoverFetchOrders(RemoteAccessException e, String customerId) {
        return Collections.emptyList(); // Return empty list as fallback
    }

    // Recovery without exception parameter (less specific)
    @Retryable(retryFor = DatabaseException.class)
    public void saveData(Data data) {
        repository.save(data);
    }

    @Recover
    public void recoverSaveData(Data data) {
        fallbackRepository.save(data);
    }
}
```

## @CircuitBreaker - Circuit Breaker Pattern

Implements the circuit breaker pattern that opens when failures occur within a timeout window and resets after a recovery period. Built on top of `@Retryable` with stateful retry.

```java
@Service
public class ExternalApiService {

    // Basic circuit breaker - opens after 3 failures within 5 seconds
    // Resets after 20 seconds
    @CircuitBreaker(
        retryFor = RemoteAccessException.class,
        maxAttempts = 3,
        openTimeout = 5000,   // 5 seconds window for failures
        resetTimeout = 20000  // 20 seconds before trying again
    )
    public String callExternalApi() {
        return externalApi.getData();
    }

    // Circuit breaker with recovery
    @CircuitBreaker(
        retryFor = ServiceUnavailableException.class,
        maxAttempts = 5,
        openTimeout = 10000,
        resetTimeout = 30000,
        recover = "fallbackCall"
    )
    public ApiResponse callCriticalService() {
        return criticalService.invoke();
    }

    @Recover
    public ApiResponse fallbackCall(ServiceUnavailableException e) {
        return ApiResponse.cached(); // Return cached response when circuit is open
    }

    // Expression-based timeouts
    @CircuitBreaker(
        retryFor = TimeoutException.class,
        maxAttemptsExpression = "#{@circuitConfig.maxAttempts}",
        openTimeoutExpression = "#{@circuitConfig.openTimeout}",
        resetTimeoutExpression = "#{@circuitConfig.resetTimeout}"
    )
    public Data fetchData() {
        return dataService.fetch();
    }

    // Circuit breaker with exception expression
    @CircuitBreaker(
        maxAttempts = 3,
        openTimeout = 5000,
        resetTimeout = 15000,
        exceptionExpression = "message.contains('rate limit')"
    )
    public Response callRateLimitedApi() {
        return rateLimitedApi.call();
    }
}
```

## RetryTemplate - Imperative Retry

Programmatic retry support for explicit control over retry behavior. Thread-safe and suitable for concurrent access.

```java
// Basic usage with default settings (3 attempts, no backoff)
RetryTemplate template = new RetryTemplate();
String result = template.execute(context -> {
    return remoteService.call();
});

// Using the fluent builder API
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(5)
    .fixedBackoff(1000)
    .retryOn(IOException.class)
    .build();

String result = template.execute(context -> {
    log.info("Attempt {} of {}", context.getRetryCount() + 1, 5);
    return httpClient.get("https://api.example.com/data");
});

// Exponential backoff with specific exceptions
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(10)
    .exponentialBackoff(100, 2, 10000)  // initial=100ms, multiplier=2, max=10s
    .retryOn(IOException.class)
    .retryOn(TimeoutException.class)
    .traversingCauses()  // Check nested exception causes
    .build();

// Timeout-based retry
RetryTemplate template = RetryTemplate.builder()
    .withTimeout(Duration.ofSeconds(30))  // Keep retrying for 30 seconds
    .fixedBackoff(Duration.ofMillis(500))
    .build();

// Infinite retry with uniform random backoff
RetryTemplate template = RetryTemplate.builder()
    .infiniteRetry()
    .retryOn(TransientException.class)
    .uniformRandomBackoff(1000, 3000)  // Random delay between 1-3 seconds
    .build();

// With recovery callback for fallback
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(3)
    .fixedBackoff(1000)
    .build();

String result = template.execute(
    context -> {
        return riskyOperation();
    },
    context -> {
        // Recovery callback - invoked when all retries exhausted
        log.warn("All retries failed, returning fallback");
        return "fallback-value";
    }
);

// Manual configuration without builder
RetryTemplate template = new RetryTemplate();

SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5,
    Map.of(
        IOException.class, true,
        IllegalStateException.class, true,
        NullPointerException.class, false
    ));
template.setRetryPolicy(retryPolicy);

ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
backOffPolicy.setInitialInterval(500);
backOffPolicy.setMultiplier(1.5);
backOffPolicy.setMaxInterval(5000);
template.setBackOffPolicy(backOffPolicy);

// Using predicate for retry decision
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(5)
    .retryOn(throwable -> {
        if (throwable instanceof HttpException httpEx) {
            return httpEx.getStatusCode() >= 500;  // Retry only server errors
        }
        return false;
    })
    .fixedBackoff(1000)
    .build();

// Not retrying specific exceptions
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(5)
    .notRetryOn(IllegalArgumentException.class)
    .notRetryOn(ValidationException.class)
    .fixedBackoff(500)
    .build();
```

## RetryContext - Access Retry State

Provides access to retry state within callbacks including retry count, last exception, and custom attributes.

```java
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(5)
    .fixedBackoff(1000)
    .build();

String result = template.execute(context -> {
    // Access retry information
    int retryCount = context.getRetryCount();
    Throwable lastException = context.getLastThrowable();

    log.info("Attempt {}, last error: {}",
        retryCount + 1,
        lastException != null ? lastException.getMessage() : "none");

    // Store custom attributes for use across retries
    if (!context.hasAttribute("startTime")) {
        context.setAttribute("startTime", System.currentTimeMillis());
    }

    // Access parent context for nested retries
    RetryContext parent = context.getParent();

    return performOperation();
});

// Accessing context from within methods using RetrySynchronizationManager
@Retryable(maxAttempts = 5)
public void retryableMethod() {
    RetryContext context = RetrySynchronizationManager.getContext();
    log.info("Current attempt: {}", context.getRetryCount() + 1);

    // Perform operation
    doWork();
}

// Disable ThreadLocal storage for virtual threads (Java 21+)
RetrySynchronizationManager.setUseThreadLocal(false);
```

## RetryListener - Lifecycle Callbacks

Interface for adding behavior during retry lifecycle. Called before first attempt, after each error, after success, and after final attempt.

```java
public class LoggingRetryListener implements RetryListener {

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        log.info("Starting retry operation: {}", callback.getLabel());
        return true;  // Return false to abort retry entirely
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context,
            RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("Retry attempt {} failed: {}",
            context.getRetryCount(), throwable.getMessage());
    }

    @Override
    public <T, E extends Throwable> void onSuccess(RetryContext context,
            RetryCallback<T, E> callback, T result) {
        log.info("Retry succeeded on attempt {}", context.getRetryCount() + 1);
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context,
            RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable != null) {
            log.error("Retry exhausted after {} attempts", context.getRetryCount());
        }
    }
}

// Register listener with RetryTemplate
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(3)
    .withListener(new LoggingRetryListener())
    .build();

// Multiple listeners
RetryTemplate template = RetryTemplate.builder()
    .maxAttempts(3)
    .withListener(new LoggingRetryListener())
    .withListener(new MetricsRetryListener(meterRegistry))
    .build();

// Register listener programmatically
RetryTemplate template = new RetryTemplate();
template.registerListener(new LoggingRetryListener());
template.registerListener(new AuditingRetryListener(), 0);  // Insert at index

// Using listener with @Retryable
@Configuration
@EnableRetry
public class AppConfig {
    @Bean
    public RetryListener loggingRetryListener() {
        return new LoggingRetryListener();
    }
}

@Service
public class MyService {
    @Retryable(listeners = "loggingRetryListener")
    public void retryableMethod() {
        // Method will use the specified listener
    }
}
```

## MetricsRetryListener - Micrometer Integration

Provides Micrometer Timer metrics for retry operations. Records duration from open() to close() with tags for name, retry count, and exception.

```java
@Configuration
public class RetryMetricsConfig {

    @Bean
    public MetricsRetryListener metricsRetryListener(MeterRegistry meterRegistry) {
        MetricsRetryListener listener = new MetricsRetryListener(meterRegistry);

        // Add static custom tags to all timers
        listener.setCustomTags(Tags.of("application", "my-app"));

        // Add dynamic tags based on retry context
        listener.setCustomTagsProvider(context -> {
            String operation = (String) context.getAttribute("operation");
            return Tags.of("operation", operation != null ? operation : "unknown");
        });

        return listener;
    }
}

// Using with RetryTemplate
@Service
public class MonitoredService {

    private final RetryTemplate retryTemplate;

    public MonitoredService(MeterRegistry meterRegistry) {
        this.retryTemplate = RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(1000)
            .withListener(new MetricsRetryListener(meterRegistry))
            .build();
    }

    public Data fetchData() {
        return retryTemplate.execute(context -> {
            context.setAttribute("operation", "fetchData");
            return dataService.getData();
        });
    }
}

// Using with @Retryable
@Service
public class AnnotatedMonitoredService {

    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000),
        listeners = "metricsRetryListener"
    )
    public Data fetchData() {
        return dataService.getData();
    }
}

// Metrics exposed as:
// spring.retry{name="fetchData",retry.count="2",exception="IOException"}
```

## Backoff Policies

Configure delay behavior between retry attempts using fixed, exponential, or random strategies.

```java
// Fixed backoff - same delay between all retries
@Retryable(backoff = @Backoff(delay = 2000))  // 2 second fixed delay
public void fixedBackoff() { }

// Exponential backoff - delay doubles each retry
@Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
public void exponentialBackoff() { }
// Delays: 1s -> 2s -> 4s -> 8s -> 10s (capped at maxDelay)

// Exponential with random jitter
@Retryable(backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000, random = true))
public void exponentialRandomBackoff() { }

// Uniform random backoff - random delay between delay and maxDelay
@Retryable(backoff = @Backoff(delay = 1000, maxDelay = 5000))
public void uniformRandomBackoff() { }

// Builder API equivalents
RetryTemplate fixed = RetryTemplate.builder()
    .fixedBackoff(2000)
    .build();

RetryTemplate exponential = RetryTemplate.builder()
    .exponentialBackoff(1000, 2.0, 10000)
    .build();

RetryTemplate exponentialRandom = RetryTemplate.builder()
    .exponentialBackoff(1000, 2.0, 10000, true)
    .build();

RetryTemplate uniformRandom = RetryTemplate.builder()
    .uniformRandomBackoff(1000, 5000)
    .build();

RetryTemplate noBackoff = RetryTemplate.builder()
    .noBackoff()  // Retry immediately
    .build();

// Duration-based configuration
RetryTemplate template = RetryTemplate.builder()
    .exponentialBackoff(Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30))
    .withTimeout(Duration.ofMinutes(5))
    .build();
```

## Custom Composed Annotations

Create reusable retry configurations using composed annotations with `@AliasFor`.

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, maxDelay = 2000, random = true)
)
public @interface LocalRetryable {

    @AliasFor(annotation = Retryable.class, attribute = "recover")
    String recover() default "";

    @AliasFor(annotation = Retryable.class, attribute = "retryFor")
    Class<? extends Throwable>[] retryFor() default {};

    @AliasFor(annotation = Retryable.class, attribute = "noRetryFor")
    Class<? extends Throwable>[] noRetryFor() default {};

    @AliasFor(annotation = Retryable.class, attribute = "label")
    String label() default "";
}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
    maxAttempts = 5,
    backoff = @Backoff(delay = 1000, maxDelay = 30000, multiplier = 1.5, random = true)
)
public @interface RemoteRetryable {

    @AliasFor(annotation = Retryable.class, attribute = "recover")
    String recover() default "";

    @AliasFor(annotation = Retryable.class, attribute = "retryFor")
    Class<? extends Throwable>[] retryFor() default {};

    @AliasFor(annotation = Retryable.class, attribute = "label")
    String label() default "";
}

// Usage
@Service
public class MyService {

    @LocalRetryable(retryFor = CacheException.class, recover = "localFallback")
    public Data getFromCache(String key) {
        return cacheService.get(key);
    }

    @RemoteRetryable(retryFor = RemoteAccessException.class)
    public Data getFromRemote(String id) {
        return remoteService.fetch(id);
    }

    @Recover
    public Data localFallback(CacheException e, String key) {
        return Data.empty();
    }
}
```

## Stateful Retry

Maintains retry state across method invocations using method arguments as cache key. Required for transaction rollback scenarios.

```java
@Service
public class TransactionalService {

    // Stateful retry - state persisted across invocations
    @Retryable(stateful = true, retryFor = OptimisticLockingException.class)
    @Transactional
    public void updateWithRetry(Long entityId, UpdateRequest request) {
        Entity entity = repository.findById(entityId).orElseThrow();
        entity.update(request);
        repository.save(entity);
    }

    @Recover
    public void recoverUpdate(OptimisticLockingException e, Long entityId, UpdateRequest request) {
        log.error("Failed to update entity {} after retries", entityId);
        throw new UpdateFailedException(entityId, e);
    }
}

// Programmatic stateful retry
RetryTemplate template = new RetryTemplate();
template.setRetryPolicy(new SimpleRetryPolicy(3));

// Create state with unique key
RetryState state = new DefaultRetryState("order-123");

try {
    template.execute(
        context -> processOrder("order-123"),
        context -> handleFailure("order-123"),
        state
    );
} catch (Exception e) {
    // Handle exception after all retries exhausted
}
```

Spring Retry is ideal for applications that need to handle transient failures gracefully, such as remote service calls, database operations with optimistic locking, or any operation that may fail temporarily but succeed on retry. It integrates seamlessly with Spring's dependency injection and AOP infrastructure.

The library provides flexibility through both declarative annotations for simple use cases and programmatic templates for complex scenarios requiring dynamic configuration. Combined with Micrometer metrics support, teams can monitor retry behavior in production and tune policies based on real-world data. Spring Retry is particularly valuable in microservices architectures where network partitions and temporary service unavailability are common.
