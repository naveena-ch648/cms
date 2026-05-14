package com.cms.exception;

import lombok.Getter;

/**
 * Thrown during login when the user has 2FA enabled.
 * Carries a short-lived pending token to be used in the second-step endpoint.
 */
@Getter
public class TwoFactorRequiredException extends RuntimeException {

    private final String pendingToken;
    private final String method;

    public TwoFactorRequiredException(String pendingToken, String method) {
        super("Two-factor authentication required");
        this.pendingToken = pendingToken;
        this.method = method;
    }
}
