package com.tronai.desktopagent.task;

import com.tronai.ai.model.ModelManager;
import com.tronai.desktopagent.protocol.ProtocolMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DesktopAgentPlannerParseTest {

    @Mock
    ModelManager modelManager;

    @Test
    void mapsSearchTextQueryArgument() {
        DesktopAgentPlanner planner = new DesktopAgentPlanner(modelManager, null);
        DesktopAgentPlanner.TaskContext ctx = emptyContext("Search text in workspace");
        DesktopAgentPlanner.PlannedAction action = planner.parseModelAction(ctx, """
                {"finish":false,"summary":"Searching","tool":"FILE","operation":"SEARCH_TEXT","arguments":{"path":".","query":"GTX 1050"}}
                """);
        assertThat(action.kind()).isEqualTo(DesktopAgentPlanner.PlannedAction.Kind.ACTION);
        assertThat(action.operation()).isEqualTo("SEARCH_TEXT");
        assertThat(action.arguments().getQuery()).isEqualTo("GTX 1050");
    }

    @Test
    void mapsPatchFindAndReplace() {
        DesktopAgentPlanner planner = new DesktopAgentPlanner(modelManager, null);
        DesktopAgentPlanner.TaskContext ctx = emptyContext("Patch a file");
        DesktopAgentPlanner.PlannedAction action = planner.parseModelAction(ctx, """
                {"finish":false,"summary":"Patching","tool":"FILE","operation":"PATCH_FILE","arguments":{"path":"a.txt","find":"old","replace":"new"}}
                """);
        assertThat(action.arguments().getFind()).isEqualTo("old");
        assertThat(action.arguments().getReplace()).isEqualTo("new");
    }

    @Test
    void rejectsSearchTextWithoutQuery() {
        DesktopAgentPlanner planner = new DesktopAgentPlanner(modelManager, null);
        DesktopAgentPlanner.TaskContext ctx = emptyContext("Search");
        DesktopAgentPlanner.PlannedAction action = planner.parseModelAction(ctx, """
                {"finish":false,"summary":"Searching","tool":"FILE","operation":"SEARCH_TEXT","arguments":{"path":"."}}
                """);
        assertThat(action.kind()).isEqualTo(DesktopAgentPlanner.PlannedAction.Kind.FAIL);
        assertThat(action.summary()).contains("query");
    }

    @Test
    void createFileWithContentBecomesWriteFile() {
        DesktopAgentPlanner planner = new DesktopAgentPlanner(modelManager, null);
        DesktopAgentPlanner.TaskContext ctx = emptyContext("Create index.html");
        DesktopAgentPlanner.PlannedAction action = planner.parseModelAction(ctx, """
                {"finish":false,"summary":"Writing","tool":"FILE","operation":"CREATE_FILE","arguments":{"path":"index.html","content":"<html></html>"}}
                """);
        assertThat(action.operation()).isEqualTo("WRITE_FILE");
        assertThat(action.arguments().getContent()).contains("html");
    }

    @Test
    void finishWithoutRequiredFilesIsRejected() throws Exception {
        DesktopAgentPlanner planner = new DesktopAgentPlanner(modelManager, null);
        var workspace = Files.createTempDirectory("tron-planner-finish");
        DesktopAgentPlanner.TaskContext ctx = new DesktopAgentPlanner.TaskContext(
                "Create index.html and style.css",
                workspace.toString(),
                List.of(),
                null,
                false,
                false,
                false,
                false,
                1,
                50,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                "RUNNING",
                1L,
                "FAST");
        // record: goal, workspace, steps, lastObservation, lastFailed, lastDenied, needsReinspect,
        // repeatedFailure, actionCount, maxActions, latestUserMessage, historySummaries, changedFiles,
        // recentResults, browserUrl, browserTitle, browserState, pageText, taskStatus, userId, aiProfile
        DesktopAgentPlanner.PlannedAction action = planner.parseModelAction(ctx, """
                {"finish":true,"summary":"Done"}
                """);
        assertThat(action.kind()).isEqualTo(DesktopAgentPlanner.PlannedAction.Kind.ACTION);
        assertThat(action.operation()).isEqualTo("LIST_FILES");
    }

    @Test
    void detectDoesNotTrapCodingCreateAsDelete() {
        assertThat(DesktopAgentPlanner.detect("Create index.html then delete unused notes"))
                .isEqualTo(DesktopAgentPlanner.Mission.LLM);
        assertThat(DesktopAgentPlanner.detect("Delete hello.txt"))
                .isEqualTo(DesktopAgentPlanner.Mission.DELETE);
    }

    @Test
    void validateToolArgumentsContract() {
        ProtocolMessage.Arguments args = new ProtocolMessage.Arguments();
        assertThat(DesktopAgentPlanner.validateToolArguments("FILE", "SEARCH_TEXT", args))
                .contains("query");
        args.setQuery("x");
        assertThat(DesktopAgentPlanner.validateToolArguments("FILE", "SEARCH_TEXT", args)).isNull();
    }

    private static DesktopAgentPlanner.TaskContext emptyContext(String goal) {
        return new DesktopAgentPlanner.TaskContext(
                goal,
                System.getProperty("java.io.tmpdir"),
                List.of(),
                null,
                false,
                false,
                false,
                false,
                0,
                50,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                "RUNNING",
                1L,
                "FAST");
    }
}

