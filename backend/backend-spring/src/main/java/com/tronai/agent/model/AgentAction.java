package com.tronai.agent.model;

/**
 * Allowlisted actions for TRON Agentic Interview Preparation.
 * The planner and runner may ONLY execute actions defined in this enum.
 */
public enum AgentAction {
    CV_ANALYSIS,
    JOB_ANALYSIS,
    GAP_ANALYSIS,
    INTERVIEW_STRATEGY,
    QUESTION_GENERATION,
    FINISH
}
