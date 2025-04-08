package org.example.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.response.auth.SignInResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/me")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    @SneakyThrows
    @GetMapping()
    public SignInResponseDto getUsername(@AuthenticationPrincipal UserDetails userDetails) {

        return new SignInResponseDto(userDetails.getUsername());
    }
}
