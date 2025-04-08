package org.example.cloudstorage.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.response.storage.ResourceInfoResponseDto;
import org.example.cloudstorage.repository.FileStorageRepository;
import org.example.cloudstorage.security.CustomUserDetails;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RequestMapping("/api/resource")
@RequiredArgsConstructor
@Validated
@RestController
public class ResourceController {
    private final FileStorageRepository fileStorageRepository;

    @GetMapping
    public ResourceInfoResponseDto resource(@RequestParam("path") @NotNull @Size(max = 200) String path, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return fileStorageRepository.getInfo(userDetails.getUserId(), path);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadResource(@AuthenticationPrincipal CustomUserDetails userDetailsImpl, @RequestParam @NotNull String path) {
        Long userId = userDetailsImpl.getUserId();
        Resource resource = fileStorageRepository.get(userId, path);
        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(resource.getFilename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }


    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@RequestParam("path") @NotNull @Size(max = 200) String path, @AuthenticationPrincipal CustomUserDetails userDetails) {
        fileStorageRepository.delete(userDetails.getUserId(), path);
    }

    @GetMapping("/move")
    public ResourceInfoResponseDto move(@RequestParam("from") @NotNull @Size(max = 200) String from, @RequestParam("to") @NotNull @Size(max = 200) String to, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return fileStorageRepository.rename(userDetails.getUserId(), from, to);
    }

    @GetMapping("/search")
    public List<ResourceInfoResponseDto> search(@RequestParam("query") @NotNull @Size(max = 200) String query, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return fileStorageRepository.search(userDetails.getUserId(), query);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfoResponseDto> upload(@RequestPart("object") List<MultipartFile> files, @AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam("path") @NotNull @Size(max = 200) String path) {
        return fileStorageRepository.save(userDetails.getUserId(), path, files);
    }

}
