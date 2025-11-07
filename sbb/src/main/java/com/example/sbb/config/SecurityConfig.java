package com.example.sbb.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.ignoringRequestMatchers("/health","/api/**"))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/document/**", "/question/**", "/sw.js", "/app.js", "/css/**").permitAll()
        .anyRequest().authenticated())
      .formLogin(Customizer.withDefaults())
      .logout(Customizer.withDefaults());
    return http.build();
  }
}
