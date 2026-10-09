import { FormEvent, RefObject, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowUp, HelpCircle, Menu, Plus, Settings, X } from 'lucide-react';
import { MonEmblem } from '../components/common/MonEmblem';
import { requestAssistantReply, AI_NOT_CONNECTED_DETAIL, AI_NOT_CONNECTED_LABEL } from '../services/chatClient';
import { ChatMessage, ChatSession, ChatStatus } from '../types/chat';

const MAX_CHARS = 2000;
const DRAFT_KEY = 'ronin-chat-draft';

export function GuestChatPage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [panel, setPanel] = useState<'help' | 'settings' | null>(null);
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [draft, setDraft] = useState(() => sessionStorage.getItem(DRAFT_KEY) || '');
  const [status, setStatus] = useState<ChatStatus>('idle');
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const composer = useRef<HTMLTextAreaElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  const active = sessions.find((session) => session.id === activeId) ?? null;
  const messages = active?.messages ?? [];

  useEffect(() => {
    sessionStorage.setItem(DRAFT_KEY, draft);
  }, [draft]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' });
  }, [messages.length, notice, error]);

  const startNewChat = () => {
    if (status === 'sending') return;
    setActiveId(null);
    setNotice('');
    setError('');
    setStatus('idle');
    setSidebarOpen(false);
    composer.current?.focus();
  };

  const openSession = (id: string) => {
    if (status === 'sending') return;
    setActiveId(id);
    setError('');
    setSidebarOpen(false);
    const session = sessions.find((item) => item.id === id);
    if (session && session.messages.length > 0) {
      setStatus('unavailable');
      setNotice(`${AI_NOT_CONNECTED_LABEL}. ${AI_NOT_CONNECTED_DETAIL}`);
      return;
    }
    setNotice('');
    setStatus('idle');
  };

  const sendText = async (text: string) => {
    const content = text.trim();
    if (!content || status === 'sending') return;
    if (content.length > MAX_CHARS) {
      setError(`Messages must be ${MAX_CHARS.toLocaleString()} characters or fewer.`);
      setStatus('error');
      return;
    }

    const now = new Date().toISOString();
    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content,
      createdAt: now,
    };
    const sessionId = activeId ?? crypto.randomUUID();
    const nextSessions = active
      ? sessions.map((session) =>
          session.id === sessionId
            ? { ...session, messages: [...session.messages, userMessage], updatedAt: now }
            : session,
        )
      : [
          {
            id: sessionId,
            title: content.replace(/\s+/g, ' ').slice(0, 48),
            messages: [userMessage],
            createdAt: now,
            updatedAt: now,
          },
          ...sessions,
        ];

    setActiveId(sessionId);
    setSessions(nextSessions);
    setDraft('');
    setError('');
    setNotice('');
    setStatus('sending');

    try {
      const result = await requestAssistantReply({
        sessionId,
        messages: nextSessions.find((session) => session.id === sessionId)?.messages ?? [userMessage],
      });
      if (result.status === 'unavailable') {
        setStatus('unavailable');
        setNotice(`${result.label}. ${result.detail}`);
        return;
      }
      setStatus(result.status);
      setError(result.detail || 'The message could not be sent.');
    } catch {
      setStatus('error');
      setError('The message was kept in this chat, but the connection could not be checked.');
    }
  };

  const recheck = async () => {
    if (!active || status === 'sending') return;
    setError('');
    setStatus('sending');
    try {
      const result = await requestAssistantReply({ sessionId: active.id, messages: active.messages });
      if (result.status === 'unavailable') {
        setStatus('unavailable');
        setNotice(`${result.label}. ${result.detail}`);
        return;
      }
      setStatus(result.status);
      setError(result.detail || 'The message could not be sent.');
    } catch {
      setStatus('error');
      setError('The message was kept in this chat, but the connection could not be checked.');
    }
  };

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    void sendText(draft);
  };

  const empty = messages.length === 0;

  return (
    <div className="h-dvh bg-[#090A0F] text-gray-100 flex overflow-hidden">
      <div className="fixed inset-0 cyber-grid opacity-40 pointer-events-none" />
      {sidebarOpen && (
        <button
          type="button"
          aria-label="Close menu"
          className="fixed inset-0 z-30 bg-black/60 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}
      <aside
        className={`fixed md:static z-40 inset-y-0 left-0 w-72 shrink-0 border-r border-white/10 bg-[#0C0E14]/95 backdrop-blur-xl flex flex-col transition-transform duration-200 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
        }`}
      >
        <div className="flex items-center justify-between px-4 py-4">
          <Link to="/" className="flex items-center gap-2.5" onClick={() => setSidebarOpen(false)}>
            <MonEmblem size={32} />
            <span className="font-display font-black tracking-wide text-white">RONIN AI</span>
          </Link>
          <button type="button" className="md:hidden text-gray-400" aria-label="Close sidebar" onClick={() => setSidebarOpen(false)}>
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="px-3">
          <button
            type="button"
            onClick={startNewChat}
            disabled={status === 'sending'}
            className="w-full flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 hover:bg-white/10 px-3 py-2.5 text-sm font-semibold disabled:opacity-50"
          >
            <Plus className="w-4 h-4 text-red-400" />
            New Chat
          </button>
        </div>
        <div className="mt-5 px-4 text-[10px] font-mono uppercase tracking-wider text-gray-500">Chat history</div>
        <div className="mt-2 px-2 flex-1 overflow-y-auto">
          {sessions.length === 0 && (
            <p className="px-2 py-2 text-xs text-gray-500">No chats yet. Messages stay in this visit only.</p>
          )}
          {sessions.map((session) => (
            <button
              key={session.id}
              type="button"
              onClick={() => openSession(session.id)}
              className={`w-full text-left rounded-xl px-3 py-2 text-sm truncate ${
                session.id === activeId ? 'bg-red-600/15 text-white' : 'text-gray-300 hover:bg-white/5'
              }`}
            >
              {session.title}
            </button>
          ))}
        </div>
        <nav className="border-t border-white/10 p-2 space-y-1">
          <Link to="/aimentor" className="block rounded-xl px-3 py-2 text-sm text-gray-200 hover:bg-white/5">
            AiMentor
          </Link>
          <Link to="/welcome" className="block rounded-xl px-3 py-2 text-sm text-gray-200 hover:bg-white/5">
            About
          </Link>
          <button type="button" onClick={() => setPanel('help')} className="w-full flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-gray-200 hover:bg-white/5">
            <HelpCircle className="w-4 h-4" /> Help
          </button>
          <button type="button" onClick={() => setPanel('settings')} className="w-full flex items-center gap-2 rounded-xl px-3 py-2 text-sm text-gray-200 hover:bg-white/5">
            <Settings className="w-4 h-4" /> Settings
          </button>
          <div className="grid grid-cols-2 gap-2 pt-2">
            <Link to="/login" className="text-center rounded-xl border border-white/10 px-2 py-2 text-xs font-bold uppercase tracking-wide hover:bg-white/5">
              Log In
            </Link>
            <Link to="/login?form=signup" className="text-center rounded-xl bg-gradient-to-r from-red-600 to-red-700 px-2 py-2 text-xs font-bold uppercase tracking-wide">
              Sign Up
            </Link>
          </div>
        </nav>
      </aside>

      <main className="relative z-10 flex-1 flex flex-col min-w-0">
        <header className="flex items-center gap-3 px-3 sm:px-5 py-3 border-b border-white/5">
          <button type="button" className="md:hidden p-2 rounded-lg hover:bg-white/5" aria-label="Open menu" onClick={() => setSidebarOpen(true)}>
            <Menu className="w-5 h-5" />
          </button>
          <div className="min-w-0">
            <div className="text-[10px] font-mono uppercase tracking-[0.2em] text-red-400">Guest chat</div>
            <div className="text-xs text-gray-500 truncate">AI Interview stays a separate live practice session.</div>
          </div>
          <div className="ml-auto rounded-full border border-amber-400/30 bg-amber-400/10 px-2.5 py-1 text-[10px] font-mono uppercase tracking-wide text-amber-200">
            Not connected yet
          </div>
        </header>

        <div className="flex-1 overflow-y-auto">
          {empty ? (
            <div className="h-full flex flex-col items-center justify-center px-4 pb-8">
              <MonEmblem size={56} />
              <h1 className="mt-6 font-display font-black text-3xl sm:text-5xl text-center text-white tracking-tight">
                Where should your journey begin?
              </h1>
              <p className="mt-3 max-w-xl text-center text-sm text-gray-400">
                Ask about careers, study plans, or Japan. You can write here without logging in.
              </p>
              <Composer
                draft={draft}
                setDraft={setDraft}
                sending={status === 'sending'}
                composer={composer}
                onSubmit={onSubmit}
                centered
              />
            </div>
          ) : (
            <div className="max-w-3xl mx-auto px-4 py-6 space-y-4">
              {messages.map((message) => (
                <article key={message.id} className="flex justify-end">
                  <div className="max-w-[85%] rounded-3xl px-4 py-3 bg-gradient-to-br from-red-700 to-red-600 text-white">
                    <p className="whitespace-pre-wrap break-words text-sm">{message.content}</p>
                  </div>
                </article>
              ))}
              {status === 'sending' && (
                <p className="text-xs font-mono text-gray-400" role="status">Checking connection...</p>
              )}
              {notice && (
                <p role="status" className="rounded-2xl border border-amber-400/30 bg-amber-400/10 px-4 py-3 text-sm text-amber-100">
                  {notice}
                </p>
              )}
              <div ref={bottomRef} />
            </div>
          )}
        </div>

        {!empty && (
          <div className="px-4 pb-[max(1rem,env(safe-area-inset-bottom))]">
            <Composer
              draft={draft}
              setDraft={setDraft}
              sending={status === 'sending'}
              composer={composer}
              onSubmit={onSubmit}
            />
          </div>
        )}

        {error && (
          <div className="px-4 pb-4">
            <div role="alert" className="max-w-3xl mx-auto rounded-2xl border border-red-500/40 bg-red-950/50 px-4 py-3 text-sm text-red-100">
              <p>{error}</p>
              {active && (
                <button type="button" onClick={() => void recheck()} className="mt-3 text-xs font-bold uppercase tracking-wide text-white">
                  Retry
                </button>
              )}
            </div>
          </div>
        )}
      </main>

      {panel && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/70 p-4" role="dialog" aria-modal="true" aria-labelledby="chat-panel-title">
          <div className="w-full max-w-lg glass-panel rounded-3xl border border-white/10 p-6">
            <div className="flex items-start justify-between gap-4">
              <h2 id="chat-panel-title" className="font-display text-2xl text-white">
                {panel === 'help' ? 'Help' : 'Settings'}
              </h2>
              <button type="button" aria-label="Close" onClick={() => setPanel(null)} className="text-gray-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>
            {panel === 'help' ? (
              <div className="mt-4 space-y-3 text-sm text-gray-300">
                <p>This page is the public guest chat. You can write a message without an account.</p>
                <p>Not connected yet. AI connection will be available soon. Nothing on this page is an AI reply.</p>
                <p>Chat history stays in this visit. Refreshing the page clears it. It is not saved to an account.</p>
                <p>AiMentor and AI Interview are separate. AI Interview is the live practice session.</p>
              </div>
            ) : (
              <div className="mt-4 space-y-3 text-sm text-gray-300">
                <p>Connection: Not connected yet.</p>
                <p>Messages are limited to {MAX_CHARS.toLocaleString()} characters in the composer.</p>
                <p>Press Enter to send. Press Shift + Enter for a new line.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function Composer({
  draft,
  setDraft,
  sending,
  composer,
  onSubmit,
  centered = false,
}: {
  draft: string;
  setDraft: (value: string) => void;
  sending: boolean;
  composer: RefObject<HTMLTextAreaElement | null>;
  onSubmit: (event: FormEvent) => void;
  centered?: boolean;
}) {
  const empty = draft.trim().length === 0;
  return (
    <form onSubmit={onSubmit} className={`w-full ${centered ? 'max-w-2xl mt-8' : 'max-w-3xl mx-auto'}`}>
      <label htmlFor="ronin-chat-input" className="sr-only">
        Ask RONIN AI anything
      </label>
      <div className="flex items-end gap-2 rounded-3xl border border-white/10 bg-[#141821] px-3 py-2 shadow-2xl shadow-black/40">
        <textarea
          id="ronin-chat-input"
          ref={composer}
          rows={1}
          value={draft}
          maxLength={MAX_CHARS}
          disabled={sending}
          placeholder="Ask RONIN AI anything..."
          className="flex-1 resize-none bg-transparent px-2 py-3 text-sm text-white placeholder:text-gray-500 focus:outline-none max-h-40 disabled:opacity-60"
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
              event.preventDefault();
              if (!empty && !sending) onSubmit(event);
            }
          }}
        />
        <button
          type="submit"
          aria-label="Send"
          disabled={sending || empty}
          className="mb-1 h-10 w-10 rounded-full bg-gradient-to-r from-red-600 to-red-700 text-white flex items-center justify-center disabled:opacity-40"
        >
          <ArrowUp className="w-4 h-4" />
        </button>
      </div>
    </form>
  );
}
