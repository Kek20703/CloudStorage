package org.example.cloudstorage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInRequestDto(
        @NotBlank
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        String username,
        @NotBlank
        @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
        String password) {
}
