package ca.umika.api.common.config;

import ca.umika.api.auth.JwtAuthenticationFilter;
import ca.umika.api.common.web.ApiErrorResponse;
import ca.umika.api.common.web.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeErrorResponse(
                                        response,
                                        objectMapper,
                                        HttpStatus.UNAUTHORIZED,
                                        "Authentication required",
                                        request.getRequestURI()
                                ))
                        .accessDeniedHandler((request, response, exception) ->
                                writeErrorResponse(
                                        response,
                                        objectMapper,
                                        HttpStatus.FORBIDDEN,
                                        "Access denied",
                                        request.getRequestURI()
                                )))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/uploads/**",
                                "/api/v1/auth/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/cart/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/locations/**",
                                "/api/v1/menu-catalog/**",
                                "/api/v1/menu-categories/**",
                                "/api/v1/menu-items/**",
                                "/api/v1/menu-item-images/**",
                                "/api/v1/menu-item-options/**",
                                "/api/v1/menu-recommendations/**",
                                "/api/v1/business-settings/effective",
                                "/api/v1/business-hours/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/orders/checkout",
                                "/api/v1/orders/redemption-preview",
                                "/api/orders/checkout",
                                "/api/orders/redemption-preview",
                                "/api/v1/payments/stripe/payment-intent",
                                "/api/v1/payments/stripe/confirm",
                                "/api/v1/payments/stripe/webhook"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/system-settings/**",
                                "/api/v1/manager/system-settings/**"
                        ).hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/manager/menus").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .requestMatchers(
                                "/api/v1/admin/**",
                                "/api/v1/admin-activity-logs/**",
                                "/api/v1/audit-logs/**",
                                "/api/v1/permission-codes/**",
                                "/api/v1/manager/permission-codes/**",
                                "/api/v1/role-menus/**",
                                "/api/v1/manager/role-menus/**",
                                "/api/v1/role-permissions/**",
                                "/api/v1/system-config-cache/**",
                                "/api/v1/admin/system-menus/**",
                                "/api/v1/manager/system-menus/**",
                                "/api/v1/manager/admin/system-menus/**",
                                "/api/v1/user-permissions/**",
                                "/api/v1/manager/user-permissions/**",
                                "/api/v1/roles/**",
                                "/api/v1/manager/roles/**",
                                "/api/v1/user-roles/**",
                                "/api/v1/manager/user-roles/**"
                        ).hasRole("ADMIN")
                        .requestMatchers("/api/v1/manager/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeErrorResponse(
            jakarta.servlet.http.HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message,
            String path
    ) throws java.io.IOException {
        ApiErrorResponse error = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                Instant.now().toString()
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResult.fail(error));
    }
}
