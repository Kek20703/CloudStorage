package org.example.cloudstorage.service;

import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.cloudstorage.dto.response.ResourceInfoResponseDto;
import org.example.cloudstorage.exception.StorageException;
import org.example.cloudstorage.repository.MinioRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cloudstorage.storage.minio", name = "enabled", havingValue = "true")
public class MinioService implements StorageService {
    private static final String USER_PREFIX_FORMAT = "user-${%d}-files/";
    private static final String RESPONSE_TYPE_FOLDER = "folder";
    private static final String RESPONSE_TYPE_FILE = "file";
    private static final boolean RECURSIVE = true;
    private static final boolean NON_RECURSIVE = false;

    private final MinioRepository minioRepository;

    @Override
    public void createDefaultUserDirectory(Long userId) {
        String path = formatUserPrefix(userId);
        minioRepository.createEmptyFolder(path);
    }

    @SneakyThrows
    @Override
    public void save(Long userId, String filename, MultipartFile file) {
        String path = formatPath(userId, filename);
        InputStream inputStream = file.getInputStream();
        minioRepository.uploadFile(path, inputStream);
    }

    @Override
    public void delete(Long userId, String filename) {
        String fullPath = formatPath(userId, filename);
        if (isDirectory(fullPath)) {
            deleteDirectory(fullPath);
            return;
        }
        minioRepository.removeObject(fullPath);
    }

    @Override
    public InputStream get(Long userId, String path) {
        String fullPath = formatPath(userId, path);
        if (isDirectory(fullPath)) {
            return getDirectory(fullPath);
        }
        return minioRepository.getFile(fullPath);
    }

    @Override
    public ResourceInfoResponseDto getInfo(Long userId, String path) {
        String fullPath = formatPath(userId, path);
        StatObjectResponse stat = minioRepository.getStat(fullPath);
        return createResourceInfoResponseDto(path, String.valueOf(stat.size()));
    }

    @Override
    public ResourceInfoResponseDto rename(Long userId, String oldPath, String newPath) {
        String oldFullPath = formatPath(userId, oldPath);
        String newFullPath = formatPath(userId, newPath);

        if (isDirectory(oldFullPath)) {
            copyDirectory(oldFullPath, newFullPath);
            deleteDirectory(oldFullPath);
        } else {
            minioRepository.copyObject(oldFullPath, newFullPath);
        }

        minioRepository.removeObject(oldFullPath);
        return getInfo(userId, newPath);
    }

    @Override
    public List<ResourceInfoResponseDto> search(Long userId, String path) {
        List<ResourceInfoResponseDto> resultList = new ArrayList<>();
        String userDirectory = formatUserPrefix(userId);
        Iterable<Result<Item>> resources = minioRepository.getListFiles(userDirectory, true);

        for (Result<Item> resultItem : resources) {

            try {
                Item item = resultItem.get();
                String responsePath = item.objectName();
                String responseSize = String.valueOf(item.size());
                ResourceInfoResponseDto responseDto = createResourceInfoResponseDto(responsePath, responseSize);
                resultList.add(responseDto);
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }

        }

        return resultList;
    }

    @Override
    public ResourceInfoResponseDto createEmptyDirectory(Long userId, String path) {
        String fullPath = formatPath(userId, path);
        minioRepository.createEmptyFolder(fullPath);
        return getInfo(userId, path);
    }

    @Override
    public List<ResourceInfoResponseDto> getDirectoryContentInfo(Long userId, String path) {
        List<ResourceInfoResponseDto> resultList = new ArrayList<>();
        String fullPath = formatPath(userId, path);
        Iterable<Result<Item>> contentList = minioRepository.getListFiles(fullPath, false);

        for (Result<Item> item : contentList) {
            try {
                Item resource = item.get();
                String responsePath = resource.objectName();
                String responseSize = String.valueOf(resource.size());
                resultList.add(
                        createResourceInfoResponseDto(responsePath, responseSize)
                );
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }
        }
        return resultList;
    }

    private InputStream getDirectory(String fullPath) {
        try {
            File zipFile = File.createTempFile("folder", ".zip");
            zipFile.deleteOnExit();

            try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(fileOutputStream))) {

                Iterable<Result<Item>> objects = minioRepository.getListFiles(fullPath, NON_RECURSIVE);
                for (Result<Item> item : objects) {
                    String objectPath = item.get().objectName();

                    if (objectPath.equals(fullPath)) {
                        continue;
                    }

                    String objectName = extractName(objectPath);
                    addFileToZip(objectPath, zipOutputStream, objectName);
                }
            }
            return new FileInputStream(zipFile);
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }

    private void addFileToZip(String filePath, ZipOutputStream zipOut, String zipEntryName) {
        try (InputStream fileStream = minioRepository.getFile(filePath)) {
            zipOut.putNextEntry(new ZipEntry(zipEntryName));
            fileStream.transferTo(zipOut);
            zipOut.closeEntry();
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }

    private void copyDirectory(String oldPath, String newPath) {
        Iterable<Result<Item>> objects = minioRepository.getListFiles(oldPath, NON_RECURSIVE);

        for (Result<Item> item : objects) {
            try {
                Item resource = item.get();
                String oldObjectPath = resource.objectName();
                String oldObjectName = extractName(oldObjectPath);
                String newObjectFullPath = newPath + "/" + oldObjectName;
                if (isDirectory(oldObjectPath)) {
                    copyDirectory(oldObjectPath, newObjectFullPath);
                    continue;
                }
                minioRepository.copyObject(oldObjectPath, newObjectFullPath);


            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }

        }
    }

    private void deleteDirectory(String path) {
        Iterable<Result<Item>> objects = minioRepository.getListFiles(path, RECURSIVE);

        for (Result<Item> item : objects) {
            try {
                Item resource = item.get();
                String objectName = resource.objectName();
                if (isDirectory(objectName)) {
                    continue;
                }
                minioRepository.removeObject(objectName);
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }

        }
    }

    private ResourceInfoResponseDto createResourceInfoResponseDto(String path, String responseSize) {
        String responsePath = extractPath(path);
        String responseName = extractName(path);
        return isDirectory(path)
                ? new ResourceInfoResponseDto(responsePath, responseName, RESPONSE_TYPE_FOLDER)
                : new ResourceInfoResponseDto(responsePath, responseName, responseSize, RESPONSE_TYPE_FILE);
    }

    private boolean isDirectory(String path) {
        return path.endsWith("/");
    }

    private String extractName(String path) {
        Path pathObj = Paths.get(path);
        return pathObj.getFileName().toString();
    }

    private String extractPath(String path) {
        Path pathObj = Paths.get(path);
        return pathObj.getParent() != null ? pathObj.getParent().toString() : "";
    }

    private String formatUserPrefix(Long userId) {
        return String.format(USER_PREFIX_FORMAT, userId);
    }

    private String formatPath(Long userId, String fileName) {
        String userPrefix = formatUserPrefix(userId);
        return userPrefix + fileName;
    }
}

