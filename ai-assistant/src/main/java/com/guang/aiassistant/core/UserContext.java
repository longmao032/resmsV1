package com.guang.aiassistant.core;

/**
 * ThreadLocal holder for the current user ID within a request lifecycle.
 * Set by ChatController before PlanningFlow execution, cleared in finally.
 */
public final class UserContext {
    private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();

    public static void setCurrentUserId(String userId) { currentUserId.set(userId); }
    public static String getCurrentUserId() { return currentUserId.get(); }
    public static void clear() { currentUserId.remove(); }

    private UserContext() {}
}
