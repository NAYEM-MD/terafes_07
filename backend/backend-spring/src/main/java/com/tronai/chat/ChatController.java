package com.tronai.chat;

import com.tronai.auth.SecurityUtils;
import com.tronai.chat.dto.ChatRequest;
import com.tronai.chat.dto.ChatResponse;
import com.tronai.chat.dto.ChatSessionDetailDto;
import com.tronai.chat.dto.ChatSessionSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for general conversation chat and persistent chat sessions.
 */
@RestController
@RequestMapping("/api/v1/chat")
@Tag(name = "Chat", description = "AI conversational chat and chat history endpoints")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Send chat message", description = "Generates assistant reply using active LLM and persists turn for authenticated user")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.processChat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    @Operation(summary = "List chat sessions", description = "Retrieves all chat sessions belonging to the authenticated user")
    public ResponseEntity<List<ChatSessionSummaryDto>> getSessions() {
        Long userId = SecurityUtils.getRequiredUserId();
        return ResponseEntity.ok(chatService.getSessionsForUser(userId));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get chat session detail", description = "Retrieves full message history of a specific session owned by user")
    public ResponseEntity<ChatSessionDetailDto> getSessionDetail(@PathVariable("sessionId") String sessionId) {
        Long userId = SecurityUtils.getRequiredUserId();
        return ResponseEntity.ok(chatService.getSessionDetailForUser(userId, sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete chat session", description = "Deletes a specific chat session owned by the authenticated user")
    public ResponseEntity<Map<String, String>> deleteSession(@PathVariable("sessionId") String sessionId) {
        Long userId = SecurityUtils.getRequiredUserId();
        chatService.deleteSessionForUser(userId, sessionId);
        return ResponseEntity.ok(Map.of("detail", "Chat session deleted successfully"));
    }
}
