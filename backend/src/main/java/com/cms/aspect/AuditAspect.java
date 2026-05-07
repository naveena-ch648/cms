package com.cms.aspect;

import com.cms.annotation.Audited;
import com.cms.entity.*;
import com.cms.security.UserPrincipal;
import com.cms.service.AuditService;
import com.cms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;
    private final UserService userService;

    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String outcome = "SUCCESS";
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            outcome = "FAILURE";
            throw t;
        } finally {
            try {
                captureEvent(audited, outcome);
            } catch (Exception e) {
                log.warn("Failed to capture audit event for {}", audited.event(), e);
            }
        }
    }

    private void captureEvent(Audited audited, String outcome) {
        User user = null;
        Organization org = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            user = userService.getByIdInternal(principal.getId());
            org = user.getOrganization();
        }

        if (org == null) {
            return;
        }

        HttpServletRequest request = getHttpRequest();
        String ipAddress = request != null ? getClientIp(request) : null;
        String userAgent = request != null ? request.getHeader("User-Agent") : null;

        auditService.logAsync(
                org, user,
                audited.event(),
                audited.category(),
                audited.resourceType().isEmpty() ? null : audited.resourceType(),
                null, null,
                outcome, null,
                ipAddress, userAgent, null
        );
    }

    private HttpServletRequest getHttpRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
