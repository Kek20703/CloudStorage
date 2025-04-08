package org.example.cloudstorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.docs.authDocs.SignInDocs;
import org.example.cloudstorage.docs.authDocs.SignUpDocs;
import org.example.cloudstorage.dto.request.SignInRequestDto;
import org.example.cloudstorage.dto.request.SignUpRequestDto;
import org.example.cloudstorage.dto.response.auth.SignInResponseDto;
import org.example.cloudstorage.dto.response.auth.SignUpResponseDto;
import org.example.cloudstorage.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/sign-up")
    @SignUpDocs
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponseDto signUp (@Validated @RequestBody SignUpRequestDto requestDto, HttpServletResponse response, HttpServletRequest request) {
        SignUpResponseDto responseDto = userService.signUp(requestDto);
        authenticateUser(responseDto.username(),requestDto.password(),request, response);
        return responseDto;
    }

    @PostMapping("/sign-in")
    @SignInDocs
    public SignInResponseDto signIn(@Validated @RequestBody SignInRequestDto requestDto,
                                    HttpServletRequest request, HttpServletResponse response) {
        authenticateUser(requestDto.username(), requestDto.password(), request, response);
        return new SignInResponseDto(requestDto.username());
    }

    private void authenticateUser(String username, String password,
                                  HttpServletRequest request, HttpServletResponse response) {
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }


}
