package com.simlogicflow.SecurityConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

        @Autowired
        private JwtAuthFilter jwtAuthFilter;

        @Autowired
        private CustomAccessDeniedHandler customAccessDeniedHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/v1/auth/**").permitAll()
                                                .requestMatchers("/health").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/users/*/courses")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO", "ESTUDIANTE",
                                                                "INSTRUCTOR", "PSEUDOPILOTO")
                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/v1/users/*/change-password")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO",
                                                                "COORDINADOR TÉCNICO", "TÉCNICO MANTENIMIENTO",
                                                                "ESTUDIANTE",
                                                                "INSTRUCTOR",
                                                                "PSEUDOPILOTO")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/users/role/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO",
                                                                "COORDINADOR TÉCNICO", "TÉCNICO MANTENIMIENTO",
                                                                "TECNICO")
                                                .requestMatchers("/api/v1/users", "/api/v1/users/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO", "COORACAD",
                                                                "COORDINADOR TÉCNICO", "TECNICO")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/courses/**",
                                                                "/api/v1/pro-courses/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO", "ESTUDIANTE",
                                                                "INSTRUCTOR", "PSEUDOPILOTO",
                                                                "COORDINADOR TÉCNICO", "TÉCNICO MANTENIMIENTO")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/maintenances/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR TÉCNICO", "TECNICO",
                                                                "TÉCNICO MANTENIMIENTO", "ESTUDIANTE", "INSTRUCTOR",
                                                                "PSEUDOPILOTO", "COORDINADOR ACADÉMICO", "COORACAD")
                                                .requestMatchers("/api/v1/courses/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO")
                                                .requestMatchers("/api/v1/pro-courses/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO")
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/v1/simulators/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO",
                                                                "COORDINADOR TÉCNICO", "TÉCNICO MANTENIMIENTO",
                                                                "TECNICO")
                                                .requestMatchers("/api/v1/simulators/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO")
                                                .requestMatchers("/api/v1/rooms/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR ACADÉMICO")
                                                .requestMatchers("/api/v1/roles/**").hasAuthority("ADMINISTRADOR")
                                                .requestMatchers("/api/v1/document-types/**")
                                                .hasAuthority("ADMINISTRADOR")
                                                .requestMatchers("/api/v1/maintenance-types/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR TÉCNICO",
                                                                "TÉCNICO MANTENIMIENTO", "TECNICO")
                                                .requestMatchers("/api/v1/maintenances/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR TÉCNICO",
                                                                "TÉCNICO MANTENIMIENTO", "TECNICO")
                                                .requestMatchers("/api/v1/maintenance-history/**")
                                                .hasAnyAuthority("ADMINISTRADOR", "COORDINADOR TÉCNICO",
                                                                "TÉCNICO MANTENIMIENTO", "TECNICO")
                                                .anyRequest().authenticated())
                                .exceptionHandling(
                                                exception -> exception.accessDeniedHandler(customAccessDeniedHandler))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
                org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
                configuration.setAllowedOrigins(java.util.List.of("*")); // En desarrollo permitimos todo; en prod
                                                                         // Railway usará
                                                                         // la variable de entorno
                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(
                                java.util.List.of("Authorization", "Content-Type", "X-Requested-With", "Accept",
                                                "Origin"));
                configuration.setExposedHeaders(java.util.List.of("Authorization"));
                configuration.setAllowCredentials(false);
                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

}
