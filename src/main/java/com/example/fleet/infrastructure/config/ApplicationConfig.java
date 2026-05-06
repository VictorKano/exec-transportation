package com.example.fleet.infrastructure.config;

import com.example.fleet.application.service.AuthenticationService;
import com.example.fleet.application.service.UserService;
import com.example.fleet.application.validator.CredentialValidator;
import com.example.fleet.application.validator.UserValidator;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.port.TokenProvider;
import com.example.fleet.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for application-layer beans.
 * Wires up UserService, UserValidator, AuthenticationService, and CredentialValidator
 * as Spring-managed beans while keeping the application layer free of Spring annotations.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public UserValidator userValidator() {
        return new UserValidator();
    }

    @Bean
    public UserService userService(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    UserValidator userValidator) {
        return new UserService(userRepository, passwordEncoder, userValidator);
    }

    @Bean
    public CredentialValidator credentialValidator() {
        return new CredentialValidator();
    }

    @Bean
    public AuthenticationService authenticationService(UserRepository userRepository,
                                                       PasswordEncoder passwordEncoder,
                                                       TokenProvider tokenProvider,
                                                       CredentialValidator credentialValidator) {
        return new AuthenticationService(userRepository, passwordEncoder, tokenProvider, credentialValidator);
    }
}
