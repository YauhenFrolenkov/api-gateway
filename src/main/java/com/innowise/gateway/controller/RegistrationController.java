package com.innowise.gateway.controller;

import com.innowise.gateway.dto.RegistrationRequest;
import com.innowise.gateway.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/register")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public Mono<Void> register(@RequestBody @Valid RegistrationRequest request) {
        return registrationService.register(request);
    }

}
