package org.example.cloudstorage.repository;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.SnowballObject;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadSnowballObjectsArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.config.MinioProperties;
import org.example.cloudstorage.dto.response.storage.FileInfoResponseDto;
import org.example.cloudstorage.dto.response.storage.FolderInfoResponseDto;
import org.example.cloudstorage.dto.response.storage.ResourceInfoResponseDto;
import org.example.cloudstorage.exception.ResourceAlreadyExistsException;
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.example.cloudstorage.exception.StorageException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
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
    private static final String RESPONSE_TYPE_DIRECTORY = "DIRECTORY";
    private static final String RESPONSE_TYPE_FILE = "FILE";
    private static final boolean RECURSIVE = true;
    private static final boolean NON_RECURSIVE = false;

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Override
    public void createDefaultUserDirectory(Long userId) {
        String path = formatUserPrefix(userId);
        createEmptyFolder(path);
    }

    @Override
    public List<ResourceInfoResponseDto> save(Long userId, String filename, List<MultipartFile> files) {
        String fullPath = formatPath(userId, filename);
        List<SnowballObject> objects = new ArrayList<>();

        for (MultipartFile file : files) {
            if (checkIfObjectExists(fullPath + file.getOriginalFilename())) {
                throw new ResourceAlreadyExistsException("Object already exists");
            }
            try {
                objects.add(new SnowballObject(
                        fullPath + file.getOriginalFilename(),
                        file.getInputStream(),
                        file.getSize(),
                        ZonedDateTime.now()));
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }
        }
        uploadSnowballObjects(objects);
        return getInfoForSnowballObjects(objects);
    }

    @Override
    public void delete(Long userId, String filename) {
        String fullPath = formatPath(userId, filename);
        if (isDirectory(fullPath)) {
            deleteDirectory(fullPath);
        }
        removeObject(fullPath);
    }

    @Override
    public Resource get(Long userId, String path) {
        String fullPath = formatPath(userId, path);
        if (isDirectory(fullPath)) {
            try {
                return getDirectory(fullPath);
            } catch (Exception e) {
                throw new StorageException(e.getMessage());
            }
        }
        return new InputStreamResource(getFile(fullPath));
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
        if (!checkIfObjectExists(oldFullPath)) {
            throw new ResourceNotFoundException("Object does not exists");
        }
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
                String objectName = extractName(item.objectName());
                if (objectName.toLowerCase().contains(path.toLowerCase())) {
                    String responsePath = removeUserPrefix(item.objectName());
                    long responseSize = item.size();
                    ResourceInfoResponseDto responseDto = createResourceInfoResponseDto(responsePath, responseSize);
                    resultList.add(responseDto);
                }
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
        if (!checkIfObjectExists(fullPath)) {
            throw new ResourceNotFoundException("Object does not exists");
        }
        Iterable<Result<Item>> contentList = getListFiles(fullPath, NON_RECURSIVE);

        for (Result<Item> item : contentList) {
            try {
                Item resource = item.get();
                String objectName = resource.objectName();
                if (objectName.equals(fullPath)) {
                    continue;
                }
                String responsePath = removeUserPrefix(objectName);
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

    private boolean checkIfObjectExists(String path) {
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(minioProperties.getBucket())
                .prefix(path)
                .build());
        return results.iterator().hasNext();
    }

    private void uploadSnowballObjects(List<SnowballObject> objects) {
        try {
            minioClient.uploadSnowballObjects(UploadSnowballObjectsArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .objects(objects)
                    .build());
        } catch (Exception e) {
            throw new StorageException(e.getMessage());
        }
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

    private Resource getDirectory(String directoryPath) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        Iterable<Result<Item>> objects = getListFiles(directoryPath, RECURSIVE);
        for (Result<Item> item : objects) {
            String objectName = item.get().objectName();

            if (objectName.equals(directoryPath) || isDirectory(objectName))
                continue;

            try (InputStream inputStream = getFile(objectName)) {
                String relativePath = objectName.substring(directoryPath.length());
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipOutputStream.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
                zipOutputStream.closeEntry();
            }
        }
        zipOutputStream.finish();
        return new ByteArrayResource(byteArrayOutputStream.toByteArray());
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

    private List<ResourceInfoResponseDto> getInfoForSnowballObjects(List<SnowballObject> objects) {
        List<ResourceInfoResponseDto> response = new ArrayList<>();
        for (SnowballObject object : objects) {
            String objectName = object.name();
            StatObjectResponse stat = getStat(objectName);
            ResourceInfoResponseDto dto = createResourceInfoResponseDto(removeUserPrefix(objectName), stat.size());
            response.add(dto);
        }
        return response;
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
                ? new FolderInfoResponseDto(responsePath, responseName + "/", RESPONSE_TYPE_DIRECTORY)
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
        return pathObj.getParent() != null
                ? pathObj.getParent().toString().replace(File.separatorChar, '/') + "/"
                : "";
    }

    private String formatUserPrefix(Long userId) {
        return String.format(USER_PREFIX_FORMAT, userId);
    }

    private String formatPath(Long userId, String fileName) {
        String userPrefix = formatUserPrefix(userId);
        return userPrefix + fileName;
    }

    private String removeUserPrefix(String path) {
        int prefixEndIndex = path.indexOf("/");
        return path.substring(prefixEndIndex + 1);
    }
}