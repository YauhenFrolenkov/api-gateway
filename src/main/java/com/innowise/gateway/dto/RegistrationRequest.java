package com.innowise.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistrationRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String surname;

    @NotBlank
    @Email
    private String email;
}