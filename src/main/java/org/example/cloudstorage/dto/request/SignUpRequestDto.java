package org.example.cloudstorage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequestDto(
        @NotBlank
        @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Username must contain only Latin letters and numbers")
        String username,
        @NotBlank
        @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]+$", message = "Password must contain Latin letters and numbers")
        String password) {
}
