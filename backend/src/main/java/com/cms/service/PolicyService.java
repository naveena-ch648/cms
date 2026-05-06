package com.cms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final ObjectMapper objectMapper;

    private static final Map<String, Object> DEFAULT_POLICIES = Map.of(
            "passwordMinLength", 8,
            "passwordRequireUppercase", true,
            "passwordRequireNumber", true,
            "passwordRequireSpecialChar", false,
            "sessionTimeoutMinutes", 30,
            "maxWorkspaces", 50,
            "maxFailedLoginAttempts", 5,
            "accountLockoutMinutes", 15
    );

    public Map<String, Object> getEffectivePolicy(String policiesJson) {
        Map<String, Object> effective = new HashMap<>(DEFAULT_POLICIES);
        if (policiesJson != null && !policiesJson.isBlank() && !policiesJson.equals("{}")) {
            try {
                Map<String, Object> overrides = objectMapper.readValue(policiesJson,
                        new TypeReference<>() {});
                effective.putAll(overrides);
            } catch (JsonProcessingException e) {
                // Fall back to defaults on invalid JSON
            }
        }
        return effective;
    }

    public String mergePolicies(String existingJson, Map<String, Object> updates) throws JsonProcessingException {
        Map<String, Object> existing = new HashMap<>();
        if (existingJson != null && !existingJson.isBlank() && !existingJson.equals("{}")) {
            existing = objectMapper.readValue(existingJson, new TypeReference<>() {});
        }
        existing.putAll(updates);
        return objectMapper.writeValueAsString(existing);
    }

    public int getPasswordMinLength(Map<String, Object> policy) {
        return ((Number) policy.getOrDefault("passwordMinLength", 8)).intValue();
    }

    public boolean getPasswordRequireUppercase(Map<String, Object> policy) {
        return (Boolean) policy.getOrDefault("passwordRequireUppercase", true);
    }

    public boolean getPasswordRequireNumber(Map<String, Object> policy) {
        return (Boolean) policy.getOrDefault("passwordRequireNumber", true);
    }

    public boolean getPasswordRequireSpecialChar(Map<String, Object> policy) {
        return (Boolean) policy.getOrDefault("passwordRequireSpecialChar", false);
    }

    public int getSessionTimeoutMinutes(Map<String, Object> policy) {
        return ((Number) policy.getOrDefault("sessionTimeoutMinutes", 30)).intValue();
    }

    public int getMaxWorkspaces(Map<String, Object> policy) {
        return ((Number) policy.getOrDefault("maxWorkspaces", 50)).intValue();
    }

    public int getMaxFailedLoginAttempts(Map<String, Object> policy) {
        return ((Number) policy.getOrDefault("maxFailedLoginAttempts", 5)).intValue();
    }

    public int getAccountLockoutMinutes(Map<String, Object> policy) {
        return ((Number) policy.getOrDefault("accountLockoutMinutes", 15)).intValue();
    }

    public Map<String, Object> getDefaults() {
        return new HashMap<>(DEFAULT_POLICIES);
    }
}
