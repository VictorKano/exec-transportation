package com.example.fleet.infrastructure.config;

import com.example.fleet.application.service.AuthenticationService;
import com.example.fleet.application.service.DriverService;
import com.example.fleet.application.service.UserService;
import com.example.fleet.application.service.VehicleService;
import com.example.fleet.application.validator.CredentialValidator;
import com.example.fleet.application.validator.DriverValidator;
import com.example.fleet.application.validator.UserValidator;
import com.example.fleet.application.validator.VehicleValidator;
import com.example.fleet.domain.port.PasswordEncoder;
import com.example.fleet.domain.port.TokenProvider;
import com.example.fleet.domain.repository.DriverRepository;
import com.example.fleet.domain.repository.UserRepository;
import com.example.fleet.domain.repository.VehicleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for application-layer beans.
 * Wires up UserService, UserValidator, DriverService, DriverValidator,
 * AuthenticationService, and CredentialValidator as Spring-managed beans
 * while keeping the application layer free of Spring annotations.
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
    public DriverValidator driverValidator() {
        return new DriverValidator();
    }

    @Bean
    public DriverService driverService(UserRepository userRepository,
                                       DriverRepository driverRepository,
                                       DriverValidator driverValidator) {
        return new DriverService(userRepository, driverRepository, driverValidator);
    }

    @Bean
    public AuthenticationService authenticationService(UserRepository userRepository,
                                                       PasswordEncoder passwordEncoder,
                                                       TokenProvider tokenProvider,
                                                       CredentialValidator credentialValidator) {
        return new AuthenticationService(userRepository, passwordEncoder, tokenProvider, credentialValidator);
    }

    @Bean
    public VehicleValidator vehicleValidator() {
        return new VehicleValidator();
    }

    @Bean
    public VehicleService vehicleService(VehicleRepository vehicleRepository,
                                         DriverRepository driverRepository,
                                         VehicleValidator vehicleValidator) {
        return new VehicleService(vehicleRepository, driverRepository, vehicleValidator);
    }
}
