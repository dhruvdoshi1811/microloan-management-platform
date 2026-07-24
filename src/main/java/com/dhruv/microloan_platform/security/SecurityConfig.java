package com.dhruv.microloan_platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security: no sessions, no CSRF (there's no browser session to forge),
 * /auth/register and /auth/login are the only unauthenticated endpoints. The
 * AuthenticationManager bean below is what AuthService uses to check credentials on login;
 * Spring Boot auto-wires it to our CustomUserDetailsService + BCryptPasswordEncoder beans.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        // Loan-product writes and application approve/reject are admin/underwriter
                        // actions - everything else on these paths (reads, submitting an
                        // application) only needs to be authenticated, same as Phase A.
                        .requestMatchers(HttpMethod.POST, "/loan-products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/loan-products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/loan-applications/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/loan-applications/*/reject").hasRole("ADMIN")
                        // The whole "Admin / Observability" endpoint group - a blanket prefix rule,
                        // simpler than the per-method matchers above since this entire group is admin-only.
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Without formLogin()/httpBasic(), Spring Security's default entry point for an
                // unauthenticated request is a bare 403. Set it explicitly to 401, since that's
                // the correct status for "no/invalid credentials" in a token-based API.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
