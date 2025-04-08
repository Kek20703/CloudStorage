package org.example.cloudstorage.controller;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.docs.userDocs.UserMeDocs;
import org.example.cloudstorage.dto.response.auth.SignInResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserController {

    @GetMapping()
    @UserMeDocs
    public SignInResponseDto getUsername(@AuthenticationPrincipal UserDetails userDetails) {
        return new SignInResponseDto(userDetails.getUsername());
    }
}
