package com.cms.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FileDeletedEvent extends ApplicationEvent {
    private final String fileUuid;
    private final String fileName;
    private final Long userId;

    public FileDeletedEvent(Object source, String fileUuid, String fileName, Long userId) {
        super(source);
        this.fileUuid = fileUuid;
        this.fileName = fileName;
        this.userId = userId;
    }
}
