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
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.example.cloudstorage.exception.StorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Repository
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cloudstorage.storage.minio", name = "enabled", havingValue = "true")
public class MinioRepository {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @SneakyThrows
    public void uploadFile(String objectName, InputStream inputStream) {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1)
                        .build());
    }

    public InputStream getFile(String objectName) {
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

    public void copyObject(String objectName, String newObjectName) throws RuntimeException {
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

    public Iterable<Result<Item>> getListFiles(String path, Boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .recursive(recursive)
                        .prefix(path)
                        .build());
    }

    public void createEmptyFolder(String folderName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(folderName)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Не удалось создать папку: " + folderName);
        }
    }


    public StatObjectResponse getStat(String objectName) {
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

    public void removeObject(String objectName) {
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
}