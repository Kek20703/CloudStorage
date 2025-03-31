package org.example.cloudstorage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.example.cloudstorage.config.MinioProperties;
import org.example.cloudstorage.dto.response.storage.ResourceInfoResponseDto;
import org.example.cloudstorage.exception.ResourceAlreadyExistsException;
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.example.cloudstorage.repository.MinioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
public class MinioRepositoryIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final String BASE_FOLDER = "folder/";
    private static final String INNER_FOLDER = "folder2/";
    private static final String FILE_NAME = "test1.txt";
    private static final String CONTENT_TYPE = "text/plain";

    private static final List<MultipartFile> MULTIPART_FILE = List.of(
            new MockMultipartFile("file1", FILE_NAME, CONTENT_TYPE, FILE_NAME.getBytes()));

    private static final List<MultipartFile> MULTIPART_FILES = List.of(
            new MockMultipartFile("file1", BASE_FOLDER + FILE_NAME, CONTENT_TYPE, FILE_NAME.getBytes()),
            new MockMultipartFile("file2", BASE_FOLDER + INNER_FOLDER + FILE_NAME, CONTENT_TYPE, FILE_NAME.getBytes())
    );

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres");

    @Container
    static MinIOContainer minIOContainer = new MinIOContainer("minio/minio")
            .withUserName("user")
            .withPassword("password");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("cloudstorage.storage.minio.enabled", () -> true);
        registry.add("cloudstorage.storage.minio.endpoint", minIOContainer::getS3URL);
        registry.add("cloudstorage.storage.minio.user", () -> "user");
        registry.add("cloudstorage.storage.minio.password", () -> "password");
        registry.add("cloudstorage.storage.minio.bucket", () -> "bucket");
    }

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private MinioRepository minioRepository;

    @BeforeEach
    void createBucket() throws Exception {
        minIOContainer.start();
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(minioProperties.getBucket())
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .build());
            minioRepository.createDefaultUserDirectory(USER_ID);
        }

    }

    @AfterEach
    void CleanUp() {
        minioRepository.delete(USER_ID, "");
        minioRepository.createDefaultUserDirectory(USER_ID);
    }

    @Test
    void testSaveFileWhenNotDuplicated() {
        List<ResourceInfoResponseDto> response = minioRepository.save(USER_ID, "", MULTIPART_FILE);

        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void testSaveDirectoryWhenNotDuplicated() {
        List<ResourceInfoResponseDto> response = minioRepository.save(USER_ID, "", MULTIPART_FILES);

        assertNotNull(response);
        assertFalse(response.isEmpty());
    }

    @Test
    void testSaveExceptionThrownWhenDuplicatedFile() {
        minioRepository.save(USER_ID, "", MULTIPART_FILE);
        assertThrows(ResourceAlreadyExistsException.class, () -> minioRepository.save(USER_ID, "", MULTIPART_FILE));
    }

    @Test
    void testSaveExceptionThrownWhenDuplicatedFolder() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);

        assertThrows(ResourceAlreadyExistsException.class, () -> minioRepository.save(USER_ID, "", MULTIPART_FILES));
    }

    @Test
    void testDeleteFile() {
        minioRepository.save(USER_ID, "", MULTIPART_FILE);

        minioRepository.delete(USER_ID, MULTIPART_FILE.get(0).getOriginalFilename());

        assertTrue(minioRepository.getDirectoryContentInfo(USER_ID, "").isEmpty());
    }

    @Test
    void testDeleteFolder() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);

        minioRepository.delete(USER_ID, BASE_FOLDER);

        assertTrue(minioRepository.getDirectoryContentInfo(USER_ID, "").isEmpty());
    }

    @Test
    void testGetFile() {
        minioRepository.save(USER_ID, "", MULTIPART_FILE);

        Resource response = minioRepository.get(USER_ID, MULTIPART_FILE.get(0).getOriginalFilename());
        assertNotNull(response);

    }

    @Test
    void testGetFolder() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);

        Resource response = minioRepository.get(USER_ID, BASE_FOLDER);
        assertNotNull(response);

    }

    @Test
    void testGetExceptionThrownWhenNonexistentFile() {
        assertThrows(ResourceNotFoundException.class, () -> minioRepository.get(USER_ID, FILE_NAME));
    }

    @Test
    void testGetExceptionThrownWhenNonexistentFolder() {
        assertThrows(ResourceNotFoundException.class, () -> minioRepository.get(USER_ID, BASE_FOLDER));
    }

    @Test
    void testRenameFileWhenNewNameIsAvailable() {
        minioRepository.save(USER_ID, "", MULTIPART_FILE);

        minioRepository.rename(USER_ID, FILE_NAME, BASE_FOLDER + FILE_NAME);

        assertNotNull(minioRepository.get(USER_ID, BASE_FOLDER + FILE_NAME));
    }

    @Test
    void testRenameFolderWhenNewNameIsAvailable() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);

        minioRepository.rename(USER_ID, BASE_FOLDER + FILE_NAME, INNER_FOLDER + FILE_NAME);

        assertNotNull(minioRepository.get(USER_ID, INNER_FOLDER + FILE_NAME));
    }

    @Test
    void testRenameThrownExceptionWhenRenameNonexistentFile() {
        assertThrows(ResourceNotFoundException.class, () -> minioRepository.rename(USER_ID, BASE_FOLDER + FILE_NAME, BASE_FOLDER + INNER_FOLDER + FILE_NAME));
    }

    @Test
    void testRenameThrownExceptionWhenRenameNonexistentFolder() {
        assertThrows(ResourceNotFoundException.class, () -> minioRepository.rename(USER_ID, BASE_FOLDER, BASE_FOLDER + INNER_FOLDER));
    }

    @Test
    void testRenameThrownExceptionWhenRenameFileAndNewNameIsNotAvailable() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);
        minioRepository.save(USER_ID, "", MULTIPART_FILE);

        assertThrows(ResourceAlreadyExistsException.class, () -> minioRepository.rename(USER_ID, FILE_NAME, BASE_FOLDER + FILE_NAME));
    }

    @Test
    void testRenameThrownExceptionWhenRenameFolderAndNewNameIsNotAvailable() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);
        minioRepository.save(USER_ID, "", MULTIPART_FILE);

        assertThrows(ResourceAlreadyExistsException.class, () -> minioRepository.rename(USER_ID, BASE_FOLDER, ""));
    }

    @Test
    void testGetInfoReturnInfoWhenObjectExists() {
        minioRepository.save(USER_ID, "", MULTIPART_FILE);

        assertNotNull(minioRepository.getInfo(USER_ID, FILE_NAME));
    }

    @Test
    void testGetInfoThrownExceptionWhenObjectDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> minioRepository.getInfo(USER_ID, FILE_NAME));
    }

    @Test
    void testCreateEmptyDirectory() {
        minioRepository.createEmptyDirectory(USER_ID, BASE_FOLDER);

        assertNotNull(minioRepository.get(USER_ID, BASE_FOLDER));
    }

    @Test
    void testSearch() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);

        var result = minioRepository.search(USER_ID, FILE_NAME);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetDirectoryContentInfoWhenDirectoryExists() {
        minioRepository.save(USER_ID, "", MULTIPART_FILES);

        var result = minioRepository.getDirectoryContentInfo(USER_ID, BASE_FOLDER);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetDirectoryContentInfoExceptionThrownWhenDirectoryDoesNotExists() {
        assertThrows(ResourceNotFoundException.class, () -> minioRepository.getDirectoryContentInfo(USER_ID, INNER_FOLDER));
    }
}
