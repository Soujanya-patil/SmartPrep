package com.smartprep.service;

/**
 * Thrown when no attention-check question can be served. {@link #isQuotaExhausted()} tells the
 * controller whether the cause was the AI daily limit (503) or some other failure (502).
 */
public class AttentionCheckException extends RuntimeException {

    private final boolean quotaExhausted;

    public AttentionCheckException(String message, boolean quotaExhausted) {
        super(message);
        this.quotaExhausted = quotaExhausted;
    }

    /** @return true if the AI daily quota was the reason no question could be served */
    public boolean isQuotaExhausted() {
        return quotaExhausted;
    }
}
