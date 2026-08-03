package com.inklusport.sports.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    @Profile("docker")
    public SecurityFilterChain filterChainDocker(HttpSecurity http) throws Exception {
        // En docker se mantiene permitAll a nivel HTTP para facilitar el arranque,
        // pero el filtro JWT debe correr para resolver el principal/roles reales;
        // sin eso las sesiones quedan bajo "anonymousUser"/"fallback-id".
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Profile("!docker")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/api/sports/**").permitAll()
                        .requestMatchers("/api/disabilities/**").permitAll()
                        .requestMatchers("/api/sport-disabilities/**").permitAll()
                        .requestMatchers("/api/events/**").permitAll()
                        .requestMatchers("/api/scheduler/**").permitAll()
                        .requestMatchers("/api/routines/trainer/**").permitAll()
                        .requestMatchers("/api/routines/*/registrations").authenticated()
                        .requestMatchers("/api/routines").permitAll()
                        .requestMatchers("/api/routines/*").permitAll()
                        .requestMatchers("/api/routine-registrations/user/**").permitAll()
                        .requestMatchers("/api/routine-registrations/**").authenticated()
                        .requestMatchers("/api/registrations/user/**").permitAll()
                        .requestMatchers("/api/registrations/**").authenticated()
                        .requestMatchers("/api/waitlist/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}