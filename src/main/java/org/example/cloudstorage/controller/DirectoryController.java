package org.example.cloudstorage.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.response.storage.ResourceInfoResponseDto;
import org.example.cloudstorage.repository.FileStorageRepository;
import org.example.cloudstorage.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
@Validated
public class DirectoryController {
    private final FileStorageRepository fileStorageRepository;

    @GetMapping
    public ResponseEntity<List<ResourceInfoResponseDto>> directory(@RequestParam("path") @NotNull @Size(max = 200) String path, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ResourceInfoResponseDto> response = fileStorageRepository.getDirectoryContentInfo(userDetails.getUserId(), path);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfoResponseDto createEmptyDirectory(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @RequestParam("path") @NotNull @Size(max = 200) String path) {
        return fileStorageRepository.createEmptyDirectory(userDetails.getUserId(), path);
    }
}
