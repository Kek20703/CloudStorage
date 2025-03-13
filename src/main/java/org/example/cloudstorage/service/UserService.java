package org.example.cloudstorage.service;

import org.example.cloudstorage.dto.request.SignUpRequestDto;
import org.example.cloudstorage.dto.response.SignUpResponseDto;
import org.springframework.http.ResponseEntity;

public interface UserService {

    ResponseEntity<SignUpResponseDto> signUp(SignUpRequestDto signUpDto);

}
