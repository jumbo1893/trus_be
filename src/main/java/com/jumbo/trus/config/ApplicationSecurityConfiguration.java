package com.jumbo.trus.config;

import com.jumbo.trus.config.entrypoint.CustomHttpStatusEntryPoint;
import com.jumbo.trus.config.security.firebase.FirebaseAuthenticationFilter;
import com.jumbo.trus.service.exceptions.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class ApplicationSecurityConfiguration {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/user/create", "/user/auth").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/.well-known/apple-app-site-association",
                                "/user/registration-setup",
                                "/user/registration-setup/v2",
                                "/strava/callback",
                                "/footbar/callback",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/ws/**"
                        ).permitAll()
                        // Dočasná kompatibilita pro starší klienty na pozadí.
                        .requestMatchers(HttpMethod.POST, "/step/background-sync").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf().disable()
                .exceptionHandling(c -> c.authenticationEntryPoint(new CustomHttpStatusEntryPoint(HttpStatus.UNAUTHORIZED, "{\"message\":\"Nelze pokracovat, nejste prihlaseny!\",\"code\":\""+ AuthException.NOT_LOGGED_IN +"\"}")))
                //.exceptionHandling(c -> c.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .sessionManagement(session -> session
                        .sessionFixation().newSession() // Zajištění nové session při přihlášení
                        .maximumSessions(1) // Omezení na jednu aktivní session
                )
                .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher(); // Zapnout správu událostí HttpSession
    }

}
