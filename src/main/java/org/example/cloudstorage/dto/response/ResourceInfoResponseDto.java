package org.example.cloudstorage.dto.response;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResourceInfoResponseDto {

    private final String path;
    private final String name;

    @JsonSetter(nulls = Nulls.SKIP)
    private final String size;
    private final String type;

    public ResourceInfoResponseDto(String path, String name, String type) {
        this.type = type;
        this.name = name;
        this.size = null;
        this.path = path;
    }

}
