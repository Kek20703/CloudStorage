package org.example.cloudstorage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "cloudstorage.storage.minio")
public class MinioProperties {
    private String bucket;

    private String endpoint;

    private String user;

    private String password;
}