package org.example.cloudstorage.controller;

import org.example.cloudstorage.dto.response.SignUpResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/me")

public class UserController {
    @GetMapping()
    public ResponseEntity<SignUpResponseDto> home(@AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(new SignUpResponseDto(userDetails.getUsername()));
    }
}
