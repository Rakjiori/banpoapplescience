package com.example.sbb.config;

import com.example.sbb.domain.user.UserSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserSecurityService userSecurityService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/signup", "/login", "/css/**", "/js/**", "/sw.js", "/image/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/notices/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/schedules/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/notices/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.PUT, "/api/notices/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.DELETE, "/api/notices/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.POST, "/api/schedules/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.DELETE, "/api/schedules/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers("/api/groups/**").authenticated()
                    .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .anyRequest().authenticated()
            )
            .formLogin(login -> login
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error=true")
                    .permitAll()
            )
            .logout(logout -> logout
                    // ✅ GET 로그아웃 허용 (AntPathRequestMatcher 제거)
                    .logoutRequestMatcher(new RegexRequestMatcher("^/logout$", "GET"))
                    .logoutSuccessUrl("/")   // 로그아웃 후 홈으로 이동
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
            );

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
