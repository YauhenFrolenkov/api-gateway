package com.innowise.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        jwtProvider = mock(JwtProvider.class);
        filter = new JwtAuthenticationFilter(jwtProvider);
        chain = mock(GatewayFilterChain.class);

        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_whiteListedPath_shouldSkipAuth() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/login")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    void filter_noAuthHeader_shouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/users")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void filter_invalidToken_shouldReturnUnauthorized() {
        when(jwtProvider.validateToken("badToken")).thenReturn(false);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer badToken")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void filter_validToken_shouldAddHeadersAndProceed() {
        when(jwtProvider.validateToken("goodToken")).thenReturn(true);
        when(jwtProvider.getUserId("goodToken")).thenReturn(1L);
        when(jwtProvider.getRole("goodToken")).thenReturn("USER");

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer goodToken")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());

        ServerWebExchange mutatedExchange =
                (ServerWebExchange) mockingDetails(chain)
                        .getInvocations()
                        .iterator()
                        .next()
                        .getArgument(0);

        String userIdHeader = mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id");
        String roleHeader = mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role");

        assertThat(userIdHeader).isEqualTo("1");
        assertThat(roleHeader).isEqualTo("USER");
    }
}
