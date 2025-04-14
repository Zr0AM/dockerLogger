package org.omnomnom.dockerlogger.configuration; // Or your preferred config package

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // For CSRF disabling
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Configure authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Allow unauthenticated GET requests to any path starting with /api/
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // Allow unauthenticated POST requests to /api/addLog
                        .requestMatchers(HttpMethod.POST, "/api/addLog").permitAll()
                        // Any other request must be authenticated (you might adjust this later)
                        .anyRequest().authenticated()
                )
                // Disable CSRF protection if you are not using browser-based forms
                // or if your clients (like Postman or scripts) don't handle CSRF tokens.
                // Be cautious disabling this in production for web applications.
                .csrf(AbstractHttpConfigurer::disable)
                // You might enable HTTP Basic or other authentication methods here if needed
                .httpBasic(withDefaults()); // Example: Enable HTTP Basic auth for other requests

        return http.build();
    }
}