package com.vfa.vault.exception;

public class LlmServiceUnavailableException extends RuntimeException {

    public LlmServiceUnavailableException(String message) {
        super(message);
    }
}
