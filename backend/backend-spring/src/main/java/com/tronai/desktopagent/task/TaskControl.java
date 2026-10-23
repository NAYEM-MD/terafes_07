package com.tronai.desktopagent.task;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskControl {

    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
    private final Set<String> paused = ConcurrentHashMap.newKeySet();
    private final Set<String> stopped = ConcurrentHashMap.newKeySet();

    public void pause(String taskId) {
        paused.add(taskId);
        wake(taskId);
    }

    public void resume(String taskId) {
        paused.remove(taskId);
        wake(taskId);
    }

    public void stop(String taskId) {
        stopped.add(taskId);
        paused.remove(taskId);
        wake(taskId);
    }

    public boolean isStopped(String taskId) {
        return stopped.contains(taskId);
    }

    /**
     * Blocks while the task is paused. Returns false when the task was stopped.
     */
    public boolean awaitRunnable(String taskId) throws InterruptedException {
        Object lock = locks.computeIfAbsent(taskId, ignored -> new Object());
        synchronized (lock) {
            while (paused.contains(taskId) && !stopped.contains(taskId)) {
                lock.wait(1000);
            }
            return !stopped.contains(taskId);
        }
    }

    private void wake(String taskId) {
        Object lock = locks.computeIfAbsent(taskId, ignored -> new Object());
        synchronized (lock) {
            lock.notifyAll();
        }
    }
}
