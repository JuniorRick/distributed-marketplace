package com.marketplace.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain gatewaySecurity(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET,
                    "/actuator/health/**", "/api/products/**", "/catalog/**", "/cart/**", "/orders/**", "/").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/carts/*", "/api/orders/*", "/api/customers/me").authenticated()
                .requestMatchers(HttpMethod.POST,
                    "/api/carts", "/api/carts/*/items", "/api/orders", "/api/customers/me").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/carts/*/items/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/carts/*/items/*").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
            .build();
    }
}
