package org.example.cloudstorage.docs.userDocs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.cloudstorage.dto.response.ErrorResponseDto;
import org.example.cloudstorage.dto.response.auth.SignInResponseDto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "SignIn", description = "SignIn user", tags = {"Storage", "Auth"})
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Success",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = SignInResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class,
                                description = "User not authenticated"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class,
                                description = "Internal server error")))})
public @interface UserMeDocs {
}