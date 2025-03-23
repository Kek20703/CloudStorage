package org.example.cloudstorage.repository;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.config.MinioProperties;
import org.example.cloudstorage.dto.response.storage.FileInfoResponseDto;
import org.example.cloudstorage.dto.response.storage.FolderInfoResponseDto;
import org.example.cloudstorage.dto.response.storage.ResourceInfoResponseDto;
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.example.cloudstorage.exception.StorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
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

@Repository
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(MinioClient.class)
public class MinioRepository implements FileStorageRepository {

    private static final String USER_PREFIX_FORMAT = "user-${%d}-files/";
    private static final String RESPONSE_TYPE_FOLDER = "folder";
    private static final String RESPONSE_TYPE_FILE = "file";
    private static final boolean RECURSIVE = true;
    private static final boolean NON_RECURSIVE = false;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void createDefaultUserDirectory(Long userId) {
        String path = formatUserPrefix(userId);
        createEmptyFolder(path);
    }

    @SneakyThrows
    @Override
    public void save(Long userId, String filename, MultipartFile file) {
        String path = formatPath(userId, filename);
        InputStream inputStream = file.getInputStream();
        uploadFile(path, inputStream);
    }

    @Override
    public void delete(Long userId, String filename) {
        String fullPath = formatPath(userId, filename);
        if (isDirectory(fullPath)) {
            deleteDirectory(fullPath);
            return;
        }
        removeObject(fullPath);
    }

    @Override
    public InputStream get(Long userId, String path) {
        String fullPath = formatPath(userId, path);
        if (isDirectory(fullPath)) {
            return getDirectory(fullPath);
        }
        return getFile(fullPath);
    }

    @Override
    public ResourceInfoResponseDto getInfo(Long userId, String path) {
        String fullPath = formatPath(userId, path);
        StatObjectResponse stat = getStat(fullPath);
        return createResourceInfoResponseDto(path, stat.size());
    }

    @Override
    public ResourceInfoResponseDto rename(Long userId, String oldPath, String newPath) {
        String oldFullPath = formatPath(userId, oldPath);
        String newFullPath = formatPath(userId, newPath);

        if (isDirectory(oldFullPath)) {
            copyDirectory(oldFullPath, newFullPath);
            deleteDirectory(oldFullPath);
        } else {
            copyObject(oldFullPath, newFullPath);
        }

        removeObject(oldFullPath);
        return getInfo(userId, newPath);
    }

    @Override
    public List<ResourceInfoResponseDto> search(Long userId, String path) {
        List<ResourceInfoResponseDto> resultList = new ArrayList<>();
        String userDirectory = formatUserPrefix(userId);
        Iterable<Result<Item>> resources = getListFiles(userDirectory, RECURSIVE);

        for (Result<Item> resultItem : resources) {

            try {
                Item item = resultItem.get();
                String responsePath = item.objectName();
                long responseSize = item.size();
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
        createEmptyFolder(fullPath);
        return getInfo(userId, path);
    }

    @Override
    public List<ResourceInfoResponseDto> getDirectoryContentInfo(Long userId, String path) {
        List<ResourceInfoResponseDto> resultList = new ArrayList<>();
        String fullPath = formatPath(userId, path);
        Iterable<Result<Item>> contentList = getListFiles(fullPath, NON_RECURSIVE);

        for (Result<Item> item : contentList) {
            try {
                Item resource = item.get();
                String responsePath = resource.objectName();
                long responseSize = resource.size();
                resultList.add(
                        createResourceInfoResponseDto(responsePath, responseSize)
                );
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }
        }
        return resultList;
    }

    @SneakyThrows
    private void uploadFile(String objectName, InputStream inputStream) {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1)
                        .build());
    }

    private InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build());
        } catch (ErrorResponseException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }

    private void copyObject(String objectName, String newObjectName) throws RuntimeException {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(newObjectName)
                    .source(CopySource.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build())
                    .build());
        } catch (Exception e) {
            throw new StorageException("Can not copy file " + objectName + " to " + newObjectName);
        }
    }

    private Iterable<Result<Item>> getListFiles(String path, Boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .recursive(recursive)
                        .prefix(path)
                        .build());
    }

    private void createEmptyFolder(String folderName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(folderName)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Can not create folder: " + folderName);
        }
    }

    private StatObjectResponse getStat(String objectName) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build());
        } catch (ErrorResponseException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }

    private void removeObject(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectName)
                            .build());
        } catch (ErrorResponseException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }

    private InputStream getDirectory(String fullPath) {
        try {
            File zipFile = File.createTempFile("folder", ".zip");
            zipFile.deleteOnExit();

            try (FileOutputStream fileOutputStream = new FileOutputStream(zipFile);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(fileOutputStream))) {

                Iterable<Result<Item>> objects = getListFiles(fullPath, NON_RECURSIVE);
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
        try (InputStream fileStream = getFile(filePath)) {
            zipOut.putNextEntry(new ZipEntry(zipEntryName));
            fileStream.transferTo(zipOut);
            zipOut.closeEntry();
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
    }

    private void copyDirectory(String oldPath, String newPath) {
        Iterable<Result<Item>> objects = getListFiles(oldPath, NON_RECURSIVE);

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
                copyObject(oldObjectPath, newObjectFullPath);


            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }

        }
    }

    private void deleteDirectory(String path) {
        Iterable<Result<Item>> objects = getListFiles(path, RECURSIVE);

        for (Result<Item> item : objects) {
            try {
                Item resource = item.get();
                String objectName = resource.objectName();
                if (isDirectory(objectName)) {
                    continue;
                }
                removeObject(objectName);
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }

        }
    }

    private ResourceInfoResponseDto createResourceInfoResponseDto(String path, long responseSize) {
        String responsePath = extractPath(path);
        String responseName = extractName(path);
        return isDirectory(path)
                ? new FolderInfoResponseDto(responsePath+"/", responseName, RESPONSE_TYPE_FOLDER)
                : new FileInfoResponseDto(responsePath, responseName, responseSize, RESPONSE_TYPE_FILE);
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