package org.example.cloudstorage.dto.response.storage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FolderInfoResponseDto implements ResourceInfoResponseDto{
    private final String path;
    private final String name;
    private final String type;
}
