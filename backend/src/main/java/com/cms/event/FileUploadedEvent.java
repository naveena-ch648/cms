package com.cms.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FileUploadedEvent extends ApplicationEvent {
    private final String fileUuid;
    private final String fileName;
    private final String folderUuid;
    private final Long userId;

    public FileUploadedEvent(Object source, String fileUuid, String fileName, String folderUuid, Long userId) {
        super(source);
        this.fileUuid = fileUuid;
        this.fileName = fileName;
        this.folderUuid = folderUuid;
        this.userId = userId;
    }
}
