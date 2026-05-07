package com.cms.event;

import com.cms.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Listens for application events and dispatches webhook notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookEventListener {

    private final WebhookService webhookService;

    @Async
    @EventListener
    public void onFileUploaded(FileUploadedEvent event) {
        webhookService.dispatchEvent("file.uploaded", UUID.randomUUID().toString(), Map.of(
                "fileId", event.getFileUuid(),
                "fileName", event.getFileName(),
                "folderId", event.getFolderUuid(),
                "uploadedBy", event.getUserId()
        ));
    }

    @Async
    @EventListener
    public void onFileDeleted(FileDeletedEvent event) {
        webhookService.dispatchEvent("file.deleted", UUID.randomUUID().toString(), Map.of(
                "fileId", event.getFileUuid(),
                "fileName", event.getFileName(),
                "deletedBy", event.getUserId()
        ));
    }

    @Async
    @EventListener
    public void onFolderCreated(FolderCreatedEvent event) {
        webhookService.dispatchEvent("folder.created", UUID.randomUUID().toString(), Map.of(
                "folderId", event.getFolderUuid(),
                "folderName", event.getFolderName(),
                "createdBy", event.getUserId()
        ));
    }
}
