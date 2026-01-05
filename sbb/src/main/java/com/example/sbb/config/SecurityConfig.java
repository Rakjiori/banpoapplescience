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
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserSecurityService userSecurityService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/", "/signup", "/login", "/privacy", "/privacy.html", "/google453e511a3b8a5555.html", "/css/**", "/js/**", "/sw.js", "/image/**", "/app.js", "/favicon.ico").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/notices/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/questions/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/schedules/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/mobile/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/push/public-key").permitAll()
                    .requestMatchers(HttpMethod.POST, "/consultations/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/notices/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.PUT, "/api/notices/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.DELETE, "/api/notices/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers("/sms/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.POST, "/api/schedules/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers(HttpMethod.DELETE, "/api/schedules/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers("/admin/groups").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .requestMatchers("/api/groups/**").authenticated()
                    .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_ROOT")
                    .anyRequest().authenticated()
            )
            .requestCache(cache -> cache.requestCache(requestCache()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/consultations/**"))
            .exceptionHandling(ex -> ex
                    .defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            new AntPathRequestMatcher("/api/**")
                    )
            )
            .formLogin(login -> login
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true) // always go home after login to avoid stale saved requests
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

    @Bean
    public RequestCache requestCache() {
        return new SelectiveRequestCache();
    }
}
