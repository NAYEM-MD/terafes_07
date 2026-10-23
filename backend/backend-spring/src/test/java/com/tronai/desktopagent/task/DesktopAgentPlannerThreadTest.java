package com.tronai.desktopagent.task;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DesktopAgentPlannerThreadTest {

    private final DesktopAgentPlanner planner = new DesktopAgentPlanner(null, null);

    @Test
    void loginPageHandsTheBrowserToTheUser() {
        String goal = "Open the test site and verify the account page. http://127.0.0.1:9/login";
        DesktopAgentPlanner.PlannedAction open = planner.plan(context(goal, List.of(), "", null, null));
        assertThat(open.operation()).isEqualTo("OPEN_URL");
        assertThat(open.arguments().getHeaded()).isTrue();
        assertThat(open.arguments().getPersistent()).isTrue();

        DesktopAgentPlanner.PlannedAction handoff = planner.plan(context(goal, List.of(
                step("Opening browser page", "OPEN_URL", "COMPLETED"),
                step("Reading page text", "GET_TEXT", "COMPLETED")
        ), "TRON Login Required Password", null, "TRON Login Required Password"));
        assertThat(handoff.operation()).isEqualTo("SET_CONTROL");
        assertThat(handoff.arguments().getBrowserControl()).isEqualTo("USER");

        DesktopAgentPlanner.PlannedAction waiting = planner.plan(context(goal, List.of(
                step("Opening browser page", "OPEN_URL", "COMPLETED"),
                step("Reading page text", "GET_TEXT", "COMPLETED"),
                step("Handing the browser to you", "SET_CONTROL", "COMPLETED")
        ), "USER", null, "TRON Login Required Password"));
        assertThat(waiting.kind()).isEqualTo(DesktopAgentPlanner.PlannedAction.Kind.WAIT_USER);
        assertThat(waiting.summary()).contains("Please log in manually");
        assertThat(waiting.summary()).contains("Do not type your password");
    }

    @Test
    void followUpOpensSettingsInTheSameSession() {
        DesktopAgentPlanner.TaskContext context = new DesktopAgentPlanner.TaskContext(
                "Open the test site and verify the account page. http://127.0.0.1:9/login",
                "C:/Dev",
                List.of(),
                "",
                false,
                false,
                false,
                false,
                4,
                50,
                "Now open the settings page.",
                List.of("Account page is available."),
                List.of(),
                "GET_TEXT: TRON Logged In",
                "http://127.0.0.1:9/account",
                "TRON Account",
                "AGENT_CONTROLLED",
                "TRON Logged In",
                "COMPLETED",
                null,
                "FAST");
        DesktopAgentPlanner.PlannedAction action = planner.plan(context);
        assertThat(action.operation()).isEqualTo("OPEN_URL");
        assertThat(action.arguments().getUrl()).isEqualTo("http://127.0.0.1:9/settings");
        assertThat(action.arguments().getPersistent()).isTrue();
    }

    @Test
    void authenticatedPageDoesNotAskForLoginAgain() {
        DesktopAgentPlanner.PlannedAction action = planner.plan(context(
                "Open the test site and verify the account page. http://127.0.0.1:9/login",
                List.of(step("Opening browser page", "OPEN_URL", "COMPLETED"),
                        step("Reading page text", "GET_TEXT", "COMPLETED")),
                "TRON Logged In",
                null,
                "TRON Logged In"));
        assertThat(action.operation()).isEqualTo("SCREENSHOT");
    }

    private static DesktopAgentPlanner.TaskContext context(String goal, List<DesktopAgentPlanner.TaskContext.StepView> steps,
                                                           String observation, String browserState, String pageText) {
        return new DesktopAgentPlanner.TaskContext(goal, "C:/Dev", steps, observation, false, false, false, false,
                steps.size(), 50, goal, List.of(), List.of(), "", null, null, browserState, pageText, "RUNNING",
                null, "FAST");
    }

    private static DesktopAgentPlanner.TaskContext.StepView step(String summary, String operation, String status) {
        return new DesktopAgentPlanner.TaskContext.StepView(summary, "BROWSER", operation, status);
    }
}
