package org.example.cloudstorage.dto.response.storage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileInfoResponseDto implements ResourceInfoResponseDto{
    private final String path;
    private final String name;
    private final long size;
    private final String type;

}
