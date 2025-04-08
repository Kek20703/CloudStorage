package org.example.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.repository.FileStorageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRegistrationEventHandler {
    private final FileStorageRepository fileStorageRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleUserRegisteredEvent(UserRegistrationEvent event) {
        fileStorageRepository.createDefaultUserDirectory(event.userId());
    }
}