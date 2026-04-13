package com.innowise.gateway.service;

import com.innowise.gateway.dto.RegistrationRequest;
import com.innowise.gateway.exception.UserServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationServiceTest {

    private MockWebServer mockWebServer;
    private RegistrationService registrationService;

    @BeforeEach
    void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        registrationService = new RegistrationService(webClient);

        ReflectionTestUtils.setField(registrationService, "authServiceUrl", mockWebServer.url("/").toString());
        ReflectionTestUtils.setField(registrationService, "userServiceUrl", mockWebServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void register_success() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("1")
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200));

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("test");
        request.setPassword("123");
        request.setName("Name");
        request.setSurname("Surname");
        request.setEmail("test@test.com");

        StepVerifier.create(registrationService.register(request))
                .verifyComplete();
    }

    @Test
    void register_userServiceFails_shouldRollback() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("1")
                .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("error"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200));

        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("test");
        request.setPassword("123");
        request.setName("Name");
        request.setSurname("Surname");
        request.setEmail("test@test.com");

        StepVerifier.create(registrationService.register(request))
                .expectError(UserServiceException.class)
                .verify();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }
}
