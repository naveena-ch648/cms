package com.cms.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class FolderCreatedEvent extends ApplicationEvent {
    private final String folderUuid;
    private final String folderName;
    private final Long userId;

    public FolderCreatedEvent(Object source, String folderUuid, String folderName, Long userId) {
        super(source);
        this.folderUuid = folderUuid;
        this.folderName = folderName;
        this.userId = userId;
    }
}
