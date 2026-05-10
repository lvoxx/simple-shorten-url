# Spring Boot

Spring Boot is a Java framework that helps create stand-alone, production-grade Spring-based applications with minimal configuration. It takes an opinionated view of the Spring platform, enabling developers to quickly build applications that can be started using `java -jar` or traditional WAR deployments. The framework provides auto-configuration, embedded servers (Tomcat, Jetty), metrics, health checks, and externalized configuration out of the box.

Spring Boot's core philosophy is convention over configuration - it automatically configures your application based on dependencies present on the classpath. This eliminates boilerplate code and XML configuration, allowing developers to focus on business logic. The framework supports both servlet-based web applications (Spring MVC) and reactive applications (Spring WebFlux), with comprehensive testing support and production-ready features through Spring Boot Actuator.

## SpringApplication - Bootstrap and Run Applications

The `SpringApplication` class provides the entry point for bootstrapping Spring Boot applications. It creates the appropriate `ApplicationContext`, registers beans, and starts the embedded web server. The static `run()` method is the most common way to start an application, but you can also customize the application by creating an instance and configuring it before running.

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class MyApplication {

    @GetMapping("/")
    String home() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        // Simple bootstrap - uses defaults
        SpringApplication.run(MyApplication.class, args);
    }
}

// Customized SpringApplication
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CustomApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CustomApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLazyInitialization(true);
        app.run(args);
    }
}
```

## SpringApplicationBuilder - Fluent Builder API

The `SpringApplicationBuilder` provides a fluent API for building applications with parent/child context hierarchies and advanced configuration. This is useful when you need multiple application contexts or want to chain configuration methods in a readable way.

```java
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.Banner;

public class MyApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(MyApplication.class)
            .bannerMode(Banner.Mode.OFF)
            .profiles("production")
            .properties("server.port=8081")
            .lazyInitialization(true)
            .run(args);
    }
}

// Parent/Child context hierarchy
import org.springframework.boot.builder.SpringApplicationBuilder;

public class HierarchyApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder()
            .sources(ParentConfig.class)
            .child(ChildConfig.class)
            .bannerMode(Banner.Mode.OFF)
            .run(args);
    }
}
```

## @RestController and @RequestMapping - Web MVC Endpoints

Spring Boot auto-configures Spring MVC when `spring-boot-starter-web` is on the classpath. Controllers annotated with `@RestController` automatically serialize return values to JSON. Use `@RequestMapping` and its variants (`@GetMapping`, `@PostMapping`, etc.) to map HTTP requests to handler methods.

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private List<User> users = new ArrayList<>();

    @GetMapping
    public List<User> getAllUsers() {
        return users;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return users.stream()
            .filter(u -> u.getId().equals(id))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        users.add(user);
        return ResponseEntity.status(201).body(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        // Update logic
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        users.removeIf(u -> u.getId().equals(id));
        return ResponseEntity.noContent().build();
    }
}

record User(Long id, String name, String email) {}
```

## Externalized Configuration - application.properties/application.yaml

Spring Boot supports externalized configuration through properties files, YAML files, environment variables, and command-line arguments. Properties follow a hierarchical override pattern where later sources override earlier ones. Use `@Value` for simple injection or `@ConfigurationProperties` for type-safe binding.

```yaml
# application.yaml
spring:
  application:
    name: my-service
  datasource:
    url: jdbc:mysql://localhost/mydb
    username: dbuser
    password: dbpass
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8080
  servlet:
    context-path: /api

logging:
  level:
    root: INFO
    org.springframework.web: DEBUG

# Profile-specific configuration
---
spring:
  config:
    activate:
      on-profile: production
  datasource:
    url: jdbc:mysql://prod-server/mydb

server:
  port: 80
```

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${custom.message:Default message}")
    private String message;
}
```

## @ConfigurationProperties - Type-Safe Configuration Binding

`@ConfigurationProperties` provides type-safe binding of configuration properties to Java objects. This approach offers validation, IDE auto-completion, and better organization of related properties. Enable scanning with `@ConfigurationPropertiesScan` or register explicitly with `@EnableConfigurationProperties`.

```java
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "my.service")
@Validated
public class MyServiceProperties {

    @NotNull
    private String name;

    private boolean enabled = true;

    private Duration timeout = Duration.ofSeconds(30);

    private List<String> servers;

    private final Security security = new Security();

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public List<String> getServers() { return servers; }
    public void setServers(List<String> servers) { this.servers = servers; }
    public Security getSecurity() { return security; }

    public static class Security {
        private String username;
        private String password;
        private List<String> roles = List.of("USER");

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }
}

// Constructor binding (immutable)
@ConfigurationProperties(prefix = "my.service")
public record MyServiceProperties(
    String name,
    boolean enabled,
    Duration timeout,
    List<String> servers,
    Security security
) {
    public record Security(String username, String password, List<String> roles) {}
}
```

```yaml
# application.yaml
my:
  service:
    name: my-application
    enabled: true
    timeout: 30s
    servers:
      - server1.example.com
      - server2.example.com
    security:
      username: admin
      password: secret
      roles:
        - ADMIN
        - USER
```

## Spring Data JPA - Repository Pattern

Spring Boot auto-configures JPA with Hibernate when `spring-boot-starter-data-jpa` is on the classpath. Define repository interfaces extending `JpaRepository` to get CRUD operations automatically. Spring Data generates query implementations from method names.

```java
import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "cities")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String country;

    // Constructors, getters, setters
    protected City() {}

    public City(String name, String state, String country) {
        this.name = name;
        this.state = state;
        this.country = country;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getState() { return state; }
    public String getCountry() { return country; }
}

public interface CityRepository extends JpaRepository<City, Long> {

    // Derived query from method name
    List<City> findAllByState(String state);

    Optional<City> findByNameAndState(String name, String state);

    List<City> findByCountryOrderByNameAsc(String country);

    // Custom JPQL query
    @Query("SELECT c FROM City c WHERE c.country = :country AND c.state = :state")
    List<City> findCitiesByCountryAndState(
        @Param("country") String country,
        @Param("state") String state
    );

    // Native SQL query
    @Query(value = "SELECT * FROM cities WHERE name LIKE %:name%", nativeQuery = true)
    List<City> searchByName(@Param("name") String name);
}

// Usage in service
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public City createCity(String name, String state, String country) {
        City city = new City(name, state, country);
        return cityRepository.save(city);
    }

    @Transactional(readOnly = true)
    public List<City> getCitiesByState(String state) {
        return cityRepository.findAllByState(state);
    }
}
```

## JdbcTemplate and JdbcClient - Direct Database Access

For simpler database operations or when JPA is overkill, use `JdbcTemplate` or the newer `JdbcClient` for direct JDBC access. Spring Boot auto-configures these when a `DataSource` is available.

```java
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepository {

    private final JdbcClient jdbcClient;

    public CustomerRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Customer> findAll() {
        return jdbcClient.sql("SELECT id, name, email FROM customers")
            .query(Customer.class)
            .list();
    }

    public Optional<Customer> findById(Long id) {
        return jdbcClient.sql("SELECT id, name, email FROM customers WHERE id = :id")
            .param("id", id)
            .query(Customer.class)
            .optional();
    }

    public int save(Customer customer) {
        return jdbcClient.sql("INSERT INTO customers (name, email) VALUES (:name, :email)")
            .param("name", customer.name())
            .param("email", customer.email())
            .update();
    }

    public int update(Customer customer) {
        return jdbcClient.sql("UPDATE customers SET name = :name, email = :email WHERE id = :id")
            .param("id", customer.id())
            .param("name", customer.name())
            .param("email", customer.email())
            .update();
    }

    public int deleteById(Long id) {
        return jdbcClient.sql("DELETE FROM customers WHERE id = :id")
            .param("id", id)
            .update();
    }
}

record Customer(Long id, String name, String email) {}
```

## RestClient - HTTP Client for REST APIs

`RestClient` provides a modern, fluent API for making HTTP requests to external REST services. Spring Boot auto-configures a `RestClient.Builder` that you can inject and customize. Use this for synchronous/imperative HTTP calls.

```java
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;

@Service
public class ExternalApiService {

    private final RestClient restClient;

    public ExternalApiService(RestClient.Builder builder) {
        this.restClient = builder
            .baseUrl("https://api.example.com")
            .defaultHeader("Authorization", "Bearer token")
            .build();
    }

    public User getUser(Long id) {
        return restClient.get()
            .uri("/users/{id}", id)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(User.class);
    }

    public User createUser(User user) {
        return restClient.post()
            .uri("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body(user)
            .retrieve()
            .body(User.class);
    }

    public void deleteUser(Long id) {
        restClient.delete()
            .uri("/users/{id}", id)
            .retrieve()
            .toBodilessEntity();
    }

    // With error handling
    public User getUserWithErrorHandling(Long id) {
        return restClient.get()
            .uri("/users/{id}", id)
            .retrieve()
            .onStatus(status -> status.value() == 404,
                (request, response) -> {
                    throw new UserNotFoundException(id);
                })
            .body(User.class);
    }
}

record User(Long id, String name, String email) {}
```

## Spring Boot Actuator - Production Monitoring

Spring Boot Actuator provides production-ready features for monitoring and managing applications. It exposes endpoints for health checks, metrics, environment info, and more. By default, only the `/health` endpoint is exposed over HTTP.

```yaml
# application.yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,beans,mappings
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
    env:
      show-values: when-authorized
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true

# Custom info
info:
  app:
    name: My Application
    version: 1.0.0
    description: A Spring Boot application
```

```bash
# Health check
curl http://localhost:8080/actuator/health
# Response: {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}

# Application info
curl http://localhost:8080/actuator/info
# Response: {"app":{"name":"My Application","version":"1.0.0"}}

# Metrics
curl http://localhost:8080/actuator/metrics
# Response: {"names":["jvm.memory.used","http.server.requests",...]}

curl http://localhost:8080/actuator/metrics/http.server.requests
# Response: Detailed HTTP request metrics

# Environment properties
curl http://localhost:8080/actuator/env

# Bean definitions
curl http://localhost:8080/actuator/beans
```

## @SpringBootTest - Integration Testing

`@SpringBootTest` creates a full application context for integration testing. Use `webEnvironment` to configure whether to start a real server, use a mock environment, or skip web configuration entirely. Combine with `@MockBean` to replace beans with mocks.

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private UserService userService;

    @Test
    void getUserShouldReturnUser() {
        User user = new User(1L, "John", "john@example.com");
        given(userService.findById(1L)).willReturn(user);

        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/1", User.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("John");
    }

    @Test
    void createUserShouldReturnCreated() {
        User newUser = new User(null, "Jane", "jane@example.com");
        User savedUser = new User(2L, "Jane", "jane@example.com");
        given(userService.save(newUser)).willReturn(savedUser);

        ResponseEntity<User> response = restTemplate.postForEntity("/api/users", newUser, User.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}

// Sliced test for web layer only
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getUserShouldReturnUser() throws Exception {
        given(userService.findById(1L)).willReturn(new User(1L, "John", "john@example.com"));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }
}
```

## ApplicationRunner and CommandLineRunner - Startup Tasks

Implement `ApplicationRunner` or `CommandLineRunner` to execute code after the application context is ready but before it starts accepting traffic. Use these for initialization tasks, data loading, or one-time operations.

```java
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DatabaseInitializer implements ApplicationRunner {

    private final UserRepository userRepository;

    public DatabaseInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin", "admin@example.com"));
            System.out.println("Default admin user created");
        }

        // Access command line arguments
        if (args.containsOption("init-demo-data")) {
            loadDemoData();
        }
    }

    private void loadDemoData() {
        // Load demo data
    }
}

@Component
@Order(2)
public class StartupValidator implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application started with arguments: " + String.join(", ", args));
        // Perform validation
    }
}
```

## Error Handling - @ControllerAdvice and Custom Error Pages

Spring Boot provides automatic error handling with a default `/error` endpoint. Customize error responses using `@ControllerAdvice` for API errors or create custom error pages in `/error/` directory for HTML responses.

```java
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Resource Not Found");
        problem.setDetail(ex.getMessage());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation Failed");
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Internal Server Error");
        problem.setDetail("An unexpected error occurred");
        return problem;
    }
}

// Custom exception
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

## Profiles - Environment-Specific Configuration

Spring Profiles allow you to segregate configuration and beans for different environments (dev, test, prod). Activate profiles via `spring.profiles.active` property, environment variable, or command-line argument.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DataSourceConfig {

    @Bean
    @Profile("development")
    public DataSource developmentDataSource() {
        // H2 in-memory database for development
        return DataSourceBuilder.create()
            .url("jdbc:h2:mem:devdb")
            .username("sa")
            .password("")
            .build();
    }

    @Bean
    @Profile("production")
    public DataSource productionDataSource() {
        // Production database
        return DataSourceBuilder.create()
            .url("jdbc:postgresql://prod-server/mydb")
            .username("produser")
            .password("prodpass")
            .build();
    }
}
```

```yaml
# application.yaml (default)
spring:
  profiles:
    active: development

---
# application-development.yaml
spring:
  config:
    activate:
      on-profile: development
  datasource:
    url: jdbc:h2:mem:devdb
  h2:
    console:
      enabled: true

---
# application-production.yaml
spring:
  config:
    activate:
      on-profile: production
  datasource:
    url: jdbc:postgresql://prod-server/mydb
```

```bash
# Activate profile via command line
java -jar myapp.jar --spring.profiles.active=production

# Or via environment variable
export SPRING_PROFILES_ACTIVE=production
java -jar myapp.jar
```

## Scheduled Tasks - @Scheduled and @EnableScheduling

Spring Boot supports scheduled task execution with `@Scheduled` annotation. Enable scheduling with `@EnableScheduling` on a configuration class. Supports fixed rate, fixed delay, and cron expressions.

```java
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Configuration
@EnableScheduling
public class SchedulingConfig {
}

@Component
public class ScheduledTasks {

    // Run every 5 seconds
    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        System.out.println("Current time: " + LocalDateTime.now());
    }

    // Run 10 seconds after previous execution completes
    @Scheduled(fixedDelay = 10000)
    public void cleanupExpiredSessions() {
        System.out.println("Cleaning up expired sessions...");
    }

    // Run at 2 AM every day
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyBackup() {
        System.out.println("Running daily backup...");
    }

    // Run every minute using cron
    @Scheduled(cron = "0 * * * * *")
    public void everyMinuteTask() {
        System.out.println("Running every minute task");
    }

    // Initial delay before first execution
    @Scheduled(initialDelay = 5000, fixedRate = 60000)
    public void taskWithInitialDelay() {
        System.out.println("Task with 5s initial delay, then every 60s");
    }
}
```

## Summary

Spring Boot excels at building microservices, REST APIs, and enterprise applications with minimal boilerplate. Its auto-configuration intelligently sets up components based on classpath dependencies, while externalized configuration through properties/YAML files enables environment-specific deployments. The embedded server support (Tomcat, Jetty) allows applications to be packaged as executable JARs, simplifying deployment to cloud platforms like Kubernetes, Cloud Foundry, and AWS.

Common integration patterns include: building RESTful APIs with `@RestController` and Spring MVC; persisting data with Spring Data JPA repositories; calling external services with `RestClient` or `WebClient`; monitoring production systems with Actuator endpoints; and testing with `@SpringBootTest` slices. The framework's starter dependencies (`spring-boot-starter-*`) bundle compatible versions of common libraries, eliminating dependency management headaches. Combined with Spring Security for authentication/authorization and Spring Cloud for distributed systems patterns, Spring Boot provides a comprehensive platform for modern Java application development.
