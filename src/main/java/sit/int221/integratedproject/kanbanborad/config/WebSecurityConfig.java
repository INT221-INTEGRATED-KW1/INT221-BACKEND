package sit.int221.integratedproject.kanbanborad.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import sit.int221.integratedproject.kanbanborad.filter.JwtAuthFilter;
import sit.int221.integratedproject.kanbanborad.services.JwtUserDetailsService;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final JwtUserDetailsService jwtUserDetailsService;

    public WebSecurityConfig(JwtAuthFilter jwtAuthFilter, JwtUserDetailsService jwtUserDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.jwtUserDetailsService = jwtUserDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(csrf -> csrf.disable())
                .authorizeRequests(authorize -> authorize
                        .requestMatchers("/login", "/token").permitAll()  // อนุญาตให้เข้าถึง /login โดยไม่ต้องล็อกอิน
                        .requestMatchers("/v3/boards/*","/v3/boards/*/tasks", "/v3/boards/*/statuses",
                                "/v3/boards/*/tasks/*", "/v3/boards/*/statuses/*", "/v3/boards/*/maximum-status").permitAll()  // อนุญาต GET tasks, statuses สำหรับทุกคน
                        .anyRequest().authenticated())          // อื่นๆ ต้องมีการ authenticate
                .httpBasic(withDefaults());                     // ใช้ httpBasic authentication
        httpSecurity.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // เพิ่ม JWT Filter
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(jwtUserDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}