export type ChatRole = 'user' | 'assistant' | 'system';

export type ChatStatus = 'idle' | 'sending' | 'unavailable' | 'error';

export interface ChatMessage {
  id: string;
  role: ChatRole;
  content: string;
  createdAt: string;
}

export interface ChatSession {
  id: string;
  title: string;
  messages: ChatMessage[];
  createdAt: string;
  updatedAt: string;
}

/** Payload the future backend will accept. The browser must not call Ollama. */
export interface ChatSendRequest {
  sessionId: string;
  messages: ChatMessage[];
}

export interface ChatSendResult {
  status: ChatStatus;
  label: string;
  detail: string;
}
