package org.example.cloudstorage;

import org.example.cloudstorage.dto.request.SignUpRequestDto;
import org.example.cloudstorage.dto.response.auth.SignUpResponseDto;
import org.example.cloudstorage.exception.UsernameIsAlreadyTakenException;
import org.example.cloudstorage.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
public class AuthIntegrationTest {

    @Autowired
    UserService userService;

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:9.6.12"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.generate-ddl", () -> true);
    }

    @Test
    public void whenValidInput_thenCreateUser() {
        SignUpRequestDto requestDto = new SignUpRequestDto("testuser", "password123");

        SignUpResponseDto responseDto = userService.signUp(requestDto);

        assertNotNull(responseDto);
        assert responseDto.username().equals(requestDto.username());
    }

    @Test
    public void whenDuplicateUsername_thenThrowsException() {
        SignUpRequestDto requestDto = new SignUpRequestDto("DuplicateUser", "hashedPassword123");
        userService.signUp(requestDto);

        assertThrows(UsernameIsAlreadyTakenException.class, () -> {
            userService.signUp(requestDto);
        });
    }

}
