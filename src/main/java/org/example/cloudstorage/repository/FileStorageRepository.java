package org.example.cloudstorage.repository;

import org.example.cloudstorage.dto.response.storage.ResourceInfoResponseDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageRepository {

    void createDefaultUserDirectory(Long userId);

    ResourceInfoResponseDto save(Long userId, String path,  List<MultipartFile>  files);

    void delete(Long userId, String path);

    Resource get(Long userId, String path);

    ResourceInfoResponseDto rename(Long userId, String oldPath, String newPath);

    ResourceInfoResponseDto getInfo(Long userId, String path);

    ResourceInfoResponseDto createEmptyDirectory(Long userId, String path);

    List<ResourceInfoResponseDto> search(Long userId, String path);

    List<ResourceInfoResponseDto> getDirectoryContentInfo(Long userId, String path);

}
