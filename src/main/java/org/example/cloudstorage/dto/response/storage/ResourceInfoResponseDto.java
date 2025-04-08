package org.example.cloudstorage.dto.response.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceInfoResponseDto {
    private String path;
    private String name;
    private Long size;
    private String type;

    public ResourceInfoResponseDto(String path, String name, String type) {
        this.type = type;
        this.name = name;
        this.path = path;
    }
}
