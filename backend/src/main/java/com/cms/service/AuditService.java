package com.cms.service;

import com.cms.entity.AuditEvent;
import com.cms.entity.Organization;
import com.cms.entity.User;
import com.cms.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public void log(Organization org, User user, String eventType, String resourceType,
                    Long resourceId, String details, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
                .organization(org)
                .user(user)
                .eventType(eventType)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditEventRepository.save(event);
    }

    public void log(Organization org, User user, String eventType) {
        log(org, user, eventType, null, null, null, null);
    }

    public void log(Organization org, User user, String eventType, String ipAddress) {
        log(org, user, eventType, null, null, null, ipAddress);
    }
}
