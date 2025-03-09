package org.example.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.request.SignInRequestDto;
import org.example.cloudstorage.dto.request.SignUpRequestDto;
import org.example.cloudstorage.dto.response.SignUpResponseDto;
import org.example.cloudstorage.entity.User;
import org.example.cloudstorage.exception.UsernameIsAlreadyTakenException;
import org.example.cloudstorage.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<SignUpResponseDto> signUp(SignUpRequestDto signUpDto) {
        User user = new User(
                signUpDto.username(),
                "USER",
                passwordEncoder.encode(signUpDto.password())
        );
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UsernameIsAlreadyTakenException("Username is already taken");
        }
        SignUpResponseDto signUpResponseDto = new SignUpResponseDto(signUpDto.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(signUpResponseDto);
    }

    @Override
    public void signOut() {

    }

    @Override
    public ResponseEntity<String> signIn(SignInRequestDto signInDto) {
        return null;
    }
}
