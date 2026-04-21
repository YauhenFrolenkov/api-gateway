package com.innowise.gateway.security;


import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter {

    private final JwtProvider jwtProvider;

    private static final String BEARER_PREFIX = "Bearer ";

    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/registrations",
            "/actuator"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        boolean valid = jwtProvider.validateToken(token);

        if (!valid) {
            return unauthorized(exchange);
        }

        Long userId = jwtProvider.getUserId(token);
        String role = jwtProvider.getRole(token);

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
