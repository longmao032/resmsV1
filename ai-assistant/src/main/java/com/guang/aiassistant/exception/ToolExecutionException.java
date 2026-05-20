package com.guang.aiassistant.exception;

/**
 * Unchecked exception thrown by tool methods on failure.
 * Caught by Spring AI ToolCallingManager at the framework layer.
 */
public class ToolExecutionException extends RuntimeException {
    public ToolExecutionException(String message) {
        super(message);
    }

    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
