package com.cms.service;

import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.UserOrganizationRole;
import com.cms.repository.PermissionRepository;
import com.cms.repository.UserOrganizationRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UserOrganizationRoleRepository userOrgRoleRepository;
    private final PermissionRepository permissionRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String PERMISSION_CACHE_PREFIX = "permissions:user:";
    private static final int CACHE_TTL_MINUTES = 30;

    public Set<String> getEffectivePermissions(Long userId, Long organizationId) {
        String cacheKey = PERMISSION_CACHE_PREFIX + userId + ":" + organizationId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Set.of(cached.split(","));
            }
        } catch (Exception e) {
            // Redis unavailable — fall through to DB lookup
        }

        UserOrganizationRole orgRole = userOrgRoleRepository
                .findByUserIdAndOrganizationId(userId, organizationId)
                .orElse(null);

        if (orgRole == null) {
            return Set.of();
        }

        Set<String> permissions = collectRolePermissions(orgRole.getRole());

        try {
            String joined = String.join(",", permissions);
            redisTemplate.opsForValue().set(cacheKey, joined, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Redis unavailable — skip caching, permissions are still resolved from DB
        }

        return permissions;
    }

    public boolean hasPermission(Long userId, Long organizationId, String permission) {
        return getEffectivePermissions(userId, organizationId).contains(permission);
    }

    public void invalidateCache(Long userId, Long organizationId) {
        String cacheKey = PERMISSION_CACHE_PREFIX + userId + ":" + organizationId;
        redisTemplate.delete(cacheKey);
    }

    private Set<String> collectRolePermissions(Role role) {
        Set<String> permissions = new HashSet<>();
        Role current = role;
        while (current != null) {
            if (current.getPermissions() != null) {
                permissions.addAll(
                        current.getPermissions().stream()
                                .map(Permission::getName)
                                .collect(Collectors.toSet())
                );
            }
            current = current.getParentRole();
        }
        return permissions;
    }
}
