package org.example.cloudstorage.docs.resourceDocs;

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
@Operation(summary = "Search resource", description = "Search resource", tags = {"Storage", "Resource"})
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
        @ApiResponse(responseCode = "404", description = "Resource not found",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class, description = "Resource not found")
                )),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class,
                                description = "Internal server error")))})
public @interface SearchResourceDocs {
}