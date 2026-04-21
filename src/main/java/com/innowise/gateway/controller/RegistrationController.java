package com.innowise.gateway.controller;

import com.innowise.gateway.dto.RegistrationRequest;
import com.innowise.gateway.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> register(@RequestBody @Valid RegistrationRequest request) {
        return registrationService.register(request);
    }

}
