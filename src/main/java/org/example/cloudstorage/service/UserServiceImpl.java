package org.example.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.request.SignUpRequestDto;
import org.example.cloudstorage.dto.response.auth.SignUpResponseDto;
import org.example.cloudstorage.entity.User;
import org.example.cloudstorage.exception.UsernameIsAlreadyTakenException;
import org.example.cloudstorage.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SignUpResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        User user = new User(
                signUpRequestDto.username(),
                passwordEncoder.encode(signUpRequestDto.password())
        );
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new UsernameIsAlreadyTakenException("Username is already taken");
        }
        return new SignUpResponseDto(signUpRequestDto.username());

    }

}
