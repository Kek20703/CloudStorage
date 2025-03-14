package org.example.cloudstorage.controller;

import org.example.cloudstorage.dto.response.SignInResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/me")
public class UserController {
    @GetMapping()
    public SignInResponseDto getUsername(@AuthenticationPrincipal UserDetails userDetails) {

        return new SignInResponseDto(userDetails.getUsername());
    }
}
