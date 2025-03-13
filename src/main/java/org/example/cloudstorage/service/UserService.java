package org.example.cloudstorage.service;

import org.example.cloudstorage.dto.request.SignUpRequestDto;
import org.example.cloudstorage.dto.response.SignUpResponseDto;

public interface UserService {

    SignUpResponseDto signUp(SignUpRequestDto signUpDto);

}
