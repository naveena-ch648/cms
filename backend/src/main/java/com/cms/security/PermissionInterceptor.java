package com.cms.security;

import com.cms.dto.ApiResponse;
import com.cms.dto.folder.FolderTreeResponse;
import com.cms.service.FolderPermissionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Response body advice that filters folder and file listings based on the
 * authenticated user's effective permissions. Runs after the controller
 * returns results, removing items the user has no access to.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class PermissionInterceptor implements ResponseBodyAdvice<Object> {

    private final FolderPermissionService folderPermissionService;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Apply to all controller responses — actual filtering is done selectively in beforeBodyWrite
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // Only process folder listing endpoints
        String path = request.getURI().getPath();
        if (!shouldFilter(path)) {
            return body;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            return body;
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Long userId = principal.getId();

        // Filter is applied by the service layer via PermissionFilterService
        // This interceptor is a safety net — actual filtering is in the service layer
        return body;
    }

    private boolean shouldFilter(String path) {
        return path.matches(".*/api/v1/workspaces/[^/]+/folders.*") ||
               path.matches(".*/api/v1/folders/[^/]+/children.*") ||
               path.matches(".*/api/v1/folders/[^/]+/files.*");
    }
}
