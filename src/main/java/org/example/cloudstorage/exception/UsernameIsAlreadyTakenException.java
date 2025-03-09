package org.example.cloudstorage.exception;

public class UsernameIsAlreadyTakenException extends RuntimeException {
    public UsernameIsAlreadyTakenException(String message) {
        super(message);
    }
}
