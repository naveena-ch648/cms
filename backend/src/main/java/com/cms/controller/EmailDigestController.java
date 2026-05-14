package com.cms.controller;

import com.cms.dto.ApiResponse;
import com.cms.dto.digest.EmailDigestPreferenceDto;
import com.cms.dto.digest.UpdateDigestPreferenceRequest;
import com.cms.security.UserPrincipal;
import com.cms.service.EmailDigestService;
import com.cms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/email-preferences")
@RequiredArgsConstructor
public class EmailDigestController {

    private final EmailDigestService emailDigestService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<EmailDigestPreferenceDto>> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                emailDigestService.getPreference(principal.getId())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<EmailDigestPreferenceDto>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateDigestPreferenceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                emailDigestService.updatePreference(
                        principal.getId(),
                        userService.getByIdInternal(principal.getId()),
                        request)));
    }
}
