package org.example.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.request.SignInRequestDto;
import org.example.cloudstorage.dto.request.SignUpRequestDto;
import org.example.cloudstorage.dto.response.SignInResponseDto;
import org.example.cloudstorage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @PostMapping("/signUp")
    public ResponseEntity<?> register(@Validated @RequestBody SignUpRequestDto requestDto) {
        return userService.signUp(requestDto);
    }

    @PostMapping("/signIn")
    public ResponseEntity<?> signIn(@Validated @RequestBody SignInRequestDto requestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestDto.username(), requestDto.password())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return ResponseEntity.status(HttpStatus.OK).body(new SignInResponseDto(requestDto.username()));
    }

    @GetMapping("/signOut")
    public ResponseEntity<?> signOut() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }
}
