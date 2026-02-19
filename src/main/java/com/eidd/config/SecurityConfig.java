package com.eidd.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.security.users:demo:demo}") String usersSpec,
            PasswordEncoder passwordEncoder) {
        List<UserDetails> users = new ArrayList<>();
        for (String entry : usersSpec.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split(":");
            if (parts.length < 2) {
                continue;
            }
            String username = parts[0].trim();
            String password = parts[1].trim();
            String roleSpec = parts.length >= 3 ? parts[2].trim() : "USER";
            String[] roles = roleSpec.isEmpty() ? new String[] { "USER" } : roleSpec.split("\\|");

            UserDetails user = User.withUsername(username)
                    .password(passwordEncoder.encode(password))
                    .roles(roles)
                    .build();
            users.add(user);
        }

        if (users.isEmpty()) {
            users.add(User.withUsername("demo")
                    .password(passwordEncoder.encode("demo"))
                    .roles("USER")
                    .build());
        }

        return new InMemoryUserDetailsManager(users);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/version", "/ping").permitAll()
                .requestMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults());

        return http.build();
    }
}
