package com.tronai.ai;

/**
 * Request-scoped generation trace. Cleared at the start and end of each HTTP request.
 */
public final class GenerationTraceHolder {

    private static final ThreadLocal<GenerationTrace> CURRENT = new ThreadLocal<>();

    private GenerationTraceHolder() {
    }

    public static void set(GenerationTrace trace) {
        CURRENT.set(trace);
    }

    public static GenerationTrace current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
