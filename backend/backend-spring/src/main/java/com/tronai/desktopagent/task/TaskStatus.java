package com.tronai.desktopagent.task;

public enum TaskStatus {
    QUEUED,
    PLANNING,
    RUNNING,
    WAITING_APPROVAL,
    WAITING_USER,
    PAUSED,
    /** Finished without independent goal-evidence verification (deterministic demos, denied paths). */
    COMPLETED,
    /** Finished and goal evidence was checked against the workspace / browser state. */
    VERIFIED,
    FAILED,
    STOPPED
}
