package org.example.cloudstorage.dto.response.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FolderInfoResponseDto implements ResourceInfoResponseDto {
    private String path;
    private String name;
    private String type;
}
