package com.innowise.gateway.service;

import com.innowise.gateway.dto.RegistrationRequest;
import com.innowise.gateway.exception.AuthServiceException;
import com.innowise.gateway.exception.UserServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final WebClient webClient;

    @Value("${services.auth.url}")
    private String authServiceUrl;

    @Value("${services.user.url}")
    private String userServiceUrl;

    public Mono<Void> register(RegistrationRequest request) {
        return registerInAuth(request)
                .flatMap(userId ->
                        registerInUser(request, userId)
                                .onErrorResume(ex ->
                                        rollbackAuth(request)
                                                .onErrorResume(rollbackEx -> {
                                                    log.error("Rollback failed for user: {}", request.getUsername(), rollbackEx);
                                                    return Mono.empty();
                                                })
                                                .then(Mono.error(ex))
                                )
                );
    }

    private Mono<Long> registerInAuth(RegistrationRequest request) {
        return webClient.post()
                .uri(authServiceUrl + "/auth/register")
                .bodyValue(Map.of(
                        "username", request.getUsername(),
                        "password", request.getPassword()
                ))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new AuthServiceException("Auth error: " + error)))
                )
                .bodyToMono(Long.class);
    }

    private Mono<Void> registerInUser(RegistrationRequest request, Long userId) {
        return webClient.post()
                .uri(userServiceUrl + "/users")
                .bodyValue(Map.of(
                        "id", userId,
                        "name", request.getName(),
                        "surname", request.getSurname(),
                        "email", request.getEmail()
                ))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(error -> Mono.error(new UserServiceException("User error: " + error)))
                )
                .bodyToMono(Void.class);
    }

    private Mono<Void> rollbackAuth(RegistrationRequest request) {
        return webClient.delete()
                .uri(authServiceUrl + "/auth/users/{username}", request.getUsername())
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Rollback successful for user: {}", request.getUsername()))
                .doOnError(err -> log.error("Rollback failed for user: {}", request.getUsername(), err));
    }
}

