package com.lapstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
                http.securityMatcher("/admin/**")
                                .authorizeHttpRequests(a -> a
                                                .requestMatchers("/admin/login").permitAll()
                                                .anyRequest().hasAuthority("ADMIN_ACCESS"))
                                .formLogin(f -> f.loginPage("/admin/login")
                                                .loginProcessingUrl("/admin/login").defaultSuccessUrl("/admin", true))
                                .logout(l -> l.logoutUrl("/admin/logout"));
                return http.build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain staffChain(HttpSecurity http) throws Exception {
                http.securityMatcher("/staff/**")
                                .authorizeHttpRequests(a -> a
                                                .requestMatchers("/staff/login").permitAll()
                                                .requestMatchers("/staff/sales/**").hasAuthority("PORTAL_SALES")
                                                .requestMatchers("/staff/consult/**").hasAuthority("PORTAL_CONSULT")
                                                .requestMatchers("/staff/warehouse/**").hasAuthority("PORTAL_WAREHOUSE")
                                                .anyRequest().denyAll())
                                .formLogin(f -> f.loginPage("/staff/login")
                                                .loginProcessingUrl("/staff/login")
                                                .failureUrl("/staff/login?error")
                                                .successHandler((req, res, auth) -> res.sendRedirect(
                                                                Portal.byPort(req.getLocalPort()).map(Portal::prefix)
                                                                                .orElse("/shop"))))
                                .logout(l -> l.logoutUrl("/staff/logout")
                                                .logoutSuccessUrl("/staff/login?logout"));
                return http.build();
        }

        @Bean
        @Order(3)
        SecurityFilterChain shopChain(HttpSecurity http) throws Exception {
                http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
                return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
