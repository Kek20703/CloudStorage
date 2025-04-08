package org.example.cloudstorage.docs.directoryDocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.cloudstorage.dto.response.ErrorResponseDto;
import org.example.cloudstorage.dto.response.storage.ResourceInfoResponseDto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "Post directory", description = "Post directory", tags = {"Storage", "Directory"})
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = ResourceInfoResponseDto.class)))),

        @ApiResponse(responseCode = "400", description = "Invalid path format",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class,
                                description = "User not authenticated"))),
        @ApiResponse(responseCode = "404", description = "Parent directory not found",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class, description = "Parent directory not found")
                )),
        @ApiResponse(responseCode = "409", description = "Resource already exists",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class, description = "Resource already exists")
                )),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class,
                                description = "Internal server error")))})
public @interface PostDirectoryDocs {
}