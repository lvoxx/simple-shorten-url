# Spring Security

Spring Security is a powerful and highly customizable authentication and access-control framework for Java applications. It is the de-facto standard for securing Spring-based applications, providing comprehensive security services for the Spring IO Platform. Spring Security 6.0+ requires Spring 6.0 as a minimum and Java 17.

The framework provides protection against attacks like session fixation, clickjacking, cross-site request forgery (CSRF), and more. It offers a declarative security model that can be configured through annotations and Java configuration, supporting a wide range of authentication mechanisms including form-based login, HTTP Basic, OAuth 2.0, SAML 2.0, LDAP, and WebAuthn/Passkeys.

## Core Interfaces

### Authentication Interface

The `Authentication` interface represents the token for an authentication request or authenticated principal. It contains the principal, credentials, authorities, and authentication status.

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

// Creating and setting authentication manually
Authentication authentication = new UsernamePasswordAuthenticationToken(
    "username",                                              // principal
    "password",                                              // credentials
    List.of(new SimpleGrantedAuthority("ROLE_USER"))        // authorities
);

SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);

// Retrieving current authentication
Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
String username = currentAuth.getName();                     // "username"
boolean isAuthenticated = currentAuth.isAuthenticated();     // true
currentAuth.getAuthorities().forEach(auth ->
    System.out.println("Authority: " + auth.getAuthority())  // "ROLE_USER"
);
```

### AuthenticationManager Interface

The `AuthenticationManager` processes authentication requests and returns a fully authenticated object with granted authorities if successful.

```java
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthenticationConfig {

    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}

// Using AuthenticationManager
@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;

    public Authentication authenticate(String username, String password) {
        try {
            Authentication request = new UsernamePasswordAuthenticationToken(username, password);
            Authentication result = authenticationManager.authenticate(request);
            SecurityContextHolder.getContext().setAuthentication(result);
            return result;
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username or password");
        }
    }
}
```

### UserDetailsService Interface

Core interface for loading user-specific data. Used by `DaoAuthenticationProvider` to load user details during authentication.

```java
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.context.annotation.Bean;

@Configuration
public class UserDetailsConfig {

    // In-memory user store (for development/testing)
    @Bean
    public UserDetailsService inMemoryUserDetailsService() {
        UserDetails user = User.withUsername("user")
            .password("{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
            .roles("USER")
            .build();

        UserDetails admin = User.withUsername("admin")
            .password("{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
            .roles("ADMIN", "USER")
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    // Custom database-backed UserDetailsService
    @Bean
    public UserDetailsService databaseUserDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
            .map(user -> User.withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
```

### AuthorizationManager Interface

Determines if an `Authentication` has access to a specific object. The modern replacement for `AccessDecisionManager`.

```java
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import java.util.function.Supplier;

// Custom AuthorizationManager implementation
public class CustomAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationResult authorize(
            Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {

        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        String requestUri = context.getRequest().getRequestURI();
        boolean hasAccess = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                          !requestUri.startsWith("/admin"));

        return new AuthorizationDecision(hasAccess);
    }
}

// Using in configuration
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/admin/**").access(new CustomAuthorizationManager())
        .anyRequest().authenticated()
    );
    return http.build();
}
```

## Web Security Configuration

### EnableWebSecurity and SecurityFilterChain

The primary way to configure web security in Spring Security 6.x using Java configuration.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Authorization rules
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**", "/css/**", "/js/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            // Form-based login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/perform_login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // HTTP Basic for API
            .httpBasic(withDefaults())
            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/perform_logout")
                .logoutSuccessUrl("/login?logout=true")
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
            )
            // CSRF configuration
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            // Session management
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            )
            // Security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'")
                )
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withUsername("user")
            .password("{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW")
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
```

### Multiple SecurityFilterChain Configuration

Configure different security rules for different URL patterns.

```java
@Configuration
@EnableWebSecurity
public class MultiSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/register").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            );
        return http.build();
    }
}
```

## Password Encoding

### PasswordEncoder and BCrypt

Spring Security provides secure password encoding using BCrypt and other algorithms.

```java
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

@Configuration
public class PasswordConfig {

    // Recommended: Delegating password encoder (supports multiple formats)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // Encodes as: {bcrypt}$2a$10$...
        // Supports: {bcrypt}, {pbkdf2}, {scrypt}, {argon2}, {sha256}
    }

    // Or use BCrypt directly
    @Bean
    public PasswordEncoder bcryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12); // strength 4-31, default 10
    }
}

// Usage example
@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public User registerUser(String username, String rawPassword) {
        String encodedPassword = passwordEncoder.encode(rawPassword);
        // encodedPassword: {bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

        User user = new User(username, encodedPassword);
        return userRepository.save(user);
    }

    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public boolean shouldUpgradePassword(String encodedPassword) {
        return passwordEncoder.upgradeEncoding(encodedPassword);
    }
}
```

## Method Security

### @PreAuthorize and @PostAuthorize Annotations

Enable method-level security with SpEL expressions.

```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.security.access.prepost.PostFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class MethodSecurityConfig {
}

@Service
public class DocumentService {

    // Check before method execution
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAllDocuments() {
        // Only ADMIN can execute
    }

    // Check with method parameters
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public Document getDocument(String username, Long documentId) {
        return documentRepository.findById(documentId).orElseThrow();
    }

    // Check after method execution with return value
    @PostAuthorize("returnObject.owner == authentication.name or hasRole('ADMIN')")
    public Document findDocument(Long id) {
        return documentRepository.findById(id).orElseThrow();
    }

    // Filter collection before method execution
    @PreFilter("filterObject.owner == authentication.name")
    public void batchUpdate(List<Document> documents) {
        documents.forEach(documentRepository::save);
    }

    // Filter collection after method execution
    @PostFilter("filterObject.owner == authentication.name or hasRole('ADMIN')")
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }

    // Complex SpEL expressions
    @PreAuthorize("hasRole('ADMIN') and #document.status == 'DRAFT'")
    public void publishDocument(Document document) {
        document.setStatus("PUBLISHED");
        documentRepository.save(document);
    }

    // Using @Secured annotation (simpler, role-based only)
    @Secured({"ROLE_USER", "ROLE_ADMIN"})
    public List<Document> listDocuments() {
        return documentRepository.findAll();
    }

    // Using JSR-250 annotations
    @RolesAllowed({"USER", "ADMIN"})
    public Document createDocument(Document document) {
        return documentRepository.save(document);
    }
}
```

## OAuth 2.0 Support

### OAuth 2.0 Login Configuration

Configure OAuth 2.0 login with providers like Google, GitHub, etc.

```java
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration
@EnableWebSecurity
public class OAuth2LoginConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/home")
                .failureUrl("/login?error")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())
                )
            );
        return http.build();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(
            googleClientRegistration(),
            githubClientRegistration()
        );
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId("google-client-id")
            .clientSecret("google-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .clientName("Google")
            .build();
    }

    private ClientRegistration githubClientRegistration() {
        return ClientRegistration.withRegistrationId("github")
            .clientId("github-client-id")
            .clientSecret("github-client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("read:user", "user:email")
            .authorizationUri("https://github.com/login/oauth/authorize")
            .tokenUri("https://github.com/login/oauth/access_token")
            .userInfoUri("https://api.github.com/user")
            .userNameAttributeName("id")
            .clientName("GitHub")
            .build();
    }
}

// Or use application.yml configuration:
// spring:
//   security:
//     oauth2:
//       client:
//         registration:
//           google:
//             client-id: google-client-id
//             client-secret: google-client-secret
//             scope: openid,profile,email
```

### OAuth 2.0 Resource Server (JWT)

Protect APIs using JWT tokens.

```java
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasAuthority("SCOPE_admin")
                .requestMatchers("/api/**").hasAuthority("SCOPE_read")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // Using JWK Set URI
        return NimbusJwtDecoder
            .withJwkSetUri("https://auth-server.example.com/.well-known/jwks.json")
            .build();

        // Or using a public key
        // return NimbusJwtDecoder.withPublicKey(rsaPublicKey).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}

// Accessing JWT claims in controllers
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "subject", jwt.getSubject(),
            "claims", jwt.getClaims(),
            "issuedAt", jwt.getIssuedAt(),
            "expiresAt", jwt.getExpiresAt()
        );
    }
}
```

## Testing Support

### SecurityMockMvcRequestPostProcessors

Spring Security provides test utilities for MockMvc testing.

```java
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Test with mock user
    @Test
    void testWithUser() throws Exception {
        mockMvc.perform(get("/api/users")
                .with(user("testuser").roles("USER")))
            .andExpect(status().isOk());
    }

    // Test with specific authorities
    @Test
    void testWithAuthorities() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .with(user("admin")
                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk());
    }

    // Test with CSRF token
    @Test
    void testPostWithCsrf() throws Exception {
        mockMvc.perform(post("/api/users")
                .with(csrf())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\"}"))
            .andExpect(status().isCreated());
    }

    // Test with JWT
    @Test
    void testWithJwt() throws Exception {
        mockMvc.perform(get("/api/resource")
                .with(jwt()
                    .jwt(jwt -> jwt
                        .subject("user123")
                        .claim("scope", "read write")
                    )
                    .authorities(new SimpleGrantedAuthority("SCOPE_read"))))
            .andExpect(status().isOk());
    }

    // Test with OAuth2 Login
    @Test
    void testWithOAuth2Login() throws Exception {
        mockMvc.perform(get("/user/profile")
                .with(oauth2Login()
                    .attributes(attrs -> attrs
                        .put("name", "Test User")
                        .put("email", "test@example.com"))
                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk());
    }

    // Test with OIDC Login
    @Test
    void testWithOidcLogin() throws Exception {
        mockMvc.perform(get("/user/profile")
                .with(oidcLogin()
                    .idToken(token -> token
                        .claim("sub", "user123")
                        .claim("email", "user@example.com"))
                    .userInfo(info -> info
                        .name("Test User"))))
            .andExpect(status().isOk());
    }

    // Test anonymous access
    @Test
    void testAnonymous() throws Exception {
        mockMvc.perform(get("/public/info")
                .with(anonymous()))
            .andExpect(status().isOk());
    }

    // Test HTTP Basic authentication
    @Test
    void testWithHttpBasic() throws Exception {
        mockMvc.perform(get("/api/secure")
                .with(httpBasic("user", "password")))
            .andExpect(status().isOk());
    }
}

// Using @WithMockUser annotation
@WebMvcTest
class AnnotationBasedSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testWithMockUser() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN", "ROLE_USER"})
    void testWithMockAdmin() throws Exception {
        mockMvc.perform(get("/api/admin"))
            .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testAnonymousAccess() throws Exception {
        mockMvc.perform(get("/public"))
            .andExpect(status().isOk());
    }
}
```

## Summary

Spring Security is the comprehensive security framework for Spring applications, providing authentication, authorization, and protection against common vulnerabilities. The core components include `Authentication` for representing authenticated principals, `AuthenticationManager` for processing authentication requests, `UserDetailsService` for loading user data, and `AuthorizationManager` for access control decisions. The framework integrates seamlessly with Spring Boot through auto-configuration and can be customized extensively via `SecurityFilterChain` beans.

For modern applications, Spring Security supports OAuth 2.0 and OpenID Connect for both client and resource server scenarios, SAML 2.0 for enterprise SSO, WebAuthn/Passkeys for passwordless authentication, and method-level security with SpEL expressions. The testing module provides comprehensive support for unit and integration testing with `SecurityMockMvcRequestPostProcessors` and annotations like `@WithMockUser`. Whether building a simple web application or a complex microservices architecture, Spring Security offers the flexibility and security features needed to protect your applications effectively.
