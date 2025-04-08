package org.example.cloudstorage.docs.authDocs;

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
@Operation(summary = "SignUp user", description = "SignUp user", tags = {"Storage", "Auth"})
@ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Success",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = SignInResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation exception",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class))),
        @ApiResponse(responseCode = "409", description = "Invalid credentials",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class,
                                description = "Invalid credentials"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDto.class,
                                description = "Internal server error")))})
public @interface SignUpDocs {
}