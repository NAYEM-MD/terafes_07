package com.tronai.desktopagent.approval;

import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets the orchestrator wait until the user approves or denies one action.
 * An early decision is kept if it arrives before the wait starts.
 */
@Component
public class ApprovalGate {

    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> waiting = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> early = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> approvalTask = new ConcurrentHashMap<>();

    public void register(String approvalId, String taskId) {
        approvalTask.put(approvalId, taskId);
    }

    public CompletableFuture<Boolean> await(String approvalId) {
        Boolean decided = early.remove(approvalId);
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (decided != null) {
            future.complete(decided);
            return future;
        }
        waiting.put(approvalId, future);
        decided = early.remove(approvalId);
        if (decided != null) {
            waiting.remove(approvalId);
            future.complete(decided);
        }
        return future;
    }

    public void complete(String approvalId, boolean approved) {
        CompletableFuture<Boolean> future = waiting.remove(approvalId);
        if (future != null) {
            future.complete(approved);
        } else {
            early.put(approvalId, approved);
        }
    }

    public void cancelTask(String taskId) {
        approvalTask.forEach((approvalId, mappedTask) -> {
            if (taskId.equals(mappedTask)) {
                complete(approvalId, false);
            }
        });
    }
}
