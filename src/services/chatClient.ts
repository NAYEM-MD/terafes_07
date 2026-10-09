import type { ChatSendRequest, ChatSendResult } from '../types/chat';

export const AI_NOT_CONNECTED_LABEL = 'Not connected yet';
export const AI_NOT_CONNECTED_DETAIL = 'AI connection will be available soon.';

/**
 * Future path: React frontend → backend API → Ollama (localhost:11434).
 * This module is the only place that should call that backend.
 * It does not contact Ollama, and it does not invent a reply.
 */
export async function requestAssistantReply(_request: ChatSendRequest): Promise<ChatSendResult> {
  return {
    status: 'unavailable',
    label: AI_NOT_CONNECTED_LABEL,
    detail: AI_NOT_CONNECTED_DETAIL,
  };
}
