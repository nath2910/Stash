package backend.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.stream.Collectors;


import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final Environment environment;

  @Value("${app.cors.allowed-origins:}")
  private String allowedOrigins;


public SecurityConfig(JwtAuthFilter jwtAuthFilter, OAuth2SuccessHandler oAuth2SuccessHandler, Environment environment) {
  this.jwtAuthFilter = jwtAuthFilter;
  this.oAuth2SuccessHandler = oAuth2SuccessHandler;
  this.environment = environment;
}

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  return http
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // ✅
      .authorizeHttpRequests(auth -> auth
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
          .requestMatchers(
              "/auth/register",
              "/auth/login",
              "/auth/forgot-password",
              "/auth/reset-password",
              "/auth/verify-email",
              "/auth/resend-verification",
              "/oauth2/**",
              "/login/**",
              "/billing/webhook",
              "/error"
          ).permitAll()
          .anyRequest().authenticated()
      )
      .oauth2Login(oauth -> oauth
          .successHandler(oAuth2SuccessHandler)
          .failureHandler((req, res, ex) -> {
            ex.printStackTrace(); // log minimal (Koyeb stdout)
            String msg = URLEncoder.encode(ex.getMessage(), StandardCharsets.UTF_8);
            res.sendRedirect("/login?error=" + msg);
          })
      )
      .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
      .build();
}


 @Bean
public CorsConfigurationSource corsConfigurationSource() {
  CorsConfiguration config = new CorsConfiguration();

  String raw = allowedOrigins;
  boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
  if (raw == null || raw.isBlank()) {
    if (isDev) {
      raw = "http://localhost:5173";
    } else {
      throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must be set when not in dev profile");
    }
  }

  var origins = Arrays.stream(raw.split(","))
      .map(String::trim)
      .filter(s -> !s.isBlank())
      .collect(Collectors.toList());

  // ✅ IMPORTANT: patterns pour accepter des domaines render/vercel si besoin
  
  config.setAllowedOrigins(origins);

  config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
  config.setAllowedHeaders(List.of("*"));
  config.setExposedHeaders(List.of("Authorization"));
  config.setAllowCredentials(false);

  UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
  source.registerCorsConfiguration("/**", config);
  return source;
}

}
