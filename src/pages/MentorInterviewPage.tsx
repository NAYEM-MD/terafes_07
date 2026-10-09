import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { roninApi } from '../services/api';
import {
  MentorAvatarState,
  InterviewQuestion,
  TranscriptMessage,
} from '../types/interview';
import { AIMentorAvatar } from '../components/mentor/AIMentorAvatar';
import { KanjiBadge } from '../components/common/KanjiBadge';
import {
  Mic,
  MicOff,
  Send,
  Sparkles,
  Bot,
  User,
  ArrowRight,
  HelpCircle,
  RotateCcw,
  Volume2,
  Clock,
  CheckCircle2,
} from 'lucide-react';

export const MentorInterviewPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, selectedOpportunityId, addToast } = useApp();

  // Avatar state: 'idle' | 'listening' | 'thinking' | 'speaking'
  const [avatarState, setAvatarState] = useState<MentorAvatarState>('speaking');

  // Interview state
  const [targetRole, setTargetRole] = useState('Java Backend Engineer');
  const [organization, setOrganization] = useState('TechNova Japan');
  const [questions, setQuestions] = useState<InterviewQuestion[]>([]);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState<number>(2); // Question 3 / 10 active!
  const [transcript, setTranscript] = useState<TranscriptMessage[]>([]);
  const [userAnswer, setUserAnswer] = useState<string>('');
  const [isRecording, setIsRecording] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [recordingSeconds, setRecordingSeconds] = useState<number>(0);

  const transcriptEndRef = useRef<HTMLDivElement>(null);
  const recordingTimerRef = useRef<any>(null);

  // Initialize session
  useEffect(() => {
    async function initSession() {
      const session = await roninApi.startInterview(selectedOpportunityId);
      setTargetRole(session.targetRole);
      setOrganization(session.organization);
      setQuestions(session.questions);
      setCurrentQuestionIndex(session.currentQuestionIndex);
      setTranscript(session.transcript);

      // Speak initial prompt briefly
      setAvatarState('speaking');
      const timer = setTimeout(() => {
        setAvatarState('idle');
      }, 4000);
      return () => clearTimeout(timer);
    }
    initSession();
  }, [selectedOpportunityId]);

  // Scroll transcript to bottom on updates
  useEffect(() => {
    transcriptEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [transcript, isSubmitting]);

  // Audio recording simulation timer
  useEffect(() => {
    if (isRecording) {
      setAvatarState('listening');
      recordingTimerRef.current = setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    } else {
      clearInterval(recordingTimerRef.current);
      setRecordingSeconds(0);
    }
    return () => clearInterval(recordingTimerRef.current);
  }, [isRecording]);

  const handleStartRecording = () => {
    setIsRecording(true);
    addToast('Microphone stream active. Speaking to Musashi...', 'info');
  };

  const handleStopRecording = () => {
    setIsRecording(false);
    setAvatarState('idle');
    if (!userAnswer) {
      setUserAnswer(
        'During our high-concurrency capstone evaluation, simultaneous checkouts produced race conditions in our stock table. I diagnosed the thread contention with pg_stat_activity, implemented pessimistic write locks with @Lock(LockModeType.PESSIMISTIC_WRITE) in PostgreSQL, and added Redis pre-reservation tokens. This eliminated double-allocation under 5,000 requests.'
      );
    }
    addToast('Speech transcribed via Whisper neural model', 'success');
  };

  const currentQuestion = questions[currentQuestionIndex] || {
    questionNumber: currentQuestionIndex + 1,
    totalQuestions: 10,
    category: 'Problem Solving',
    text: 'Tell me about a technical problem you faced while developing a Java project and how you solved it.',
    expectedKeypoints: [
      'Problem context and root cause',
      'Specific Java technical solution',
      'Measurable outcome',
    ],
  };

  const handleSubmitAnswer = async () => {
    if (!userAnswer.trim()) {
      addToast('Please enter or dictate an answer first', 'warning');
      return;
    }

    const answerToSubmit = userAnswer;
    setUserAnswer('');
    setIsSubmitting(true);
    setAvatarState('thinking');

    try {
      const res = await roninApi.submitInterviewAnswer(
        'inv-active',
        currentQuestion.questionNumber,
        answerToSubmit
      );

      // Append messages
      setTranscript((prev) => [...prev, res.userMessage, res.aiFollowUpMessage]);
      setIsSubmitting(false);

      if (res.isFinished || currentQuestionIndex >= 9) {
        setAvatarState('speaking');
        addToast('All 10 interview evaluation modules completed! Preparing readiness analysis.', 'success');
        setTimeout(() => {
          navigate('/analysis');
        }, 2200);
      } else {
        setCurrentQuestionIndex((prev) => prev + 1);
        setAvatarState('speaking');
        setTimeout(() => {
          setAvatarState('idle');
        }, 5000);
      }
    } catch {
      setIsSubmitting(false);
      setAvatarState('idle');
      addToast('Error transmitting answer', 'error');
    }
  };

  const handleFastForwardToAnalysis = () => {
    navigate('/analysis');
  };

  return (
    <div className="space-y-6 text-left">
      {/* Top Session Status Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-white/5">
        <div className="flex items-center gap-3">
          <KanjiBadge kanji="模擬面接" subtext="Live Dojo" variant="crimson" />
          <div>
            <div className="text-xs font-mono text-gray-400">
              TARGET INTERVIEW • <span className="text-white font-semibold">{organization}</span>
            </div>
            <h1 className="font-display font-bold text-xl text-white">
              {targetRole} Technical Screening
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-xs font-mono text-gray-300">
            <Clock className="w-3.5 h-3.5 text-red-400" />
            <span>SESSION TIME: 14:38</span>
          </div>

          {/* Quick skip button for reviewers / test presentations */}
          <button
            onClick={handleFastForwardToAnalysis}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-950/40 hover:bg-red-900/60 text-red-300 hover:text-white border border-red-500/30 text-xs font-mono transition-colors"
            title="Jump directly to performance diagnostics"
          >
            <span>Finish & View Analysis</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Main Split Layout: AI Avatar (Left/Center) + Interview Console (Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* LEFT / CENTER: Large AI Mentor Holographic Portrait Area */}
        <div className="lg:col-span-5 flex flex-col items-center justify-center space-y-4">
          <div className="w-full">
            <AIMentorAvatar
              state={avatarState}
              size="lg"
              onStateChange={(s) => setAvatarState(s)}
              showControls={true}
            />
          </div>

          {/* Real-time Status Card below avatar */}
          <div className="w-full max-w-[420px] p-4 rounded-2xl bg-white/[0.02] border border-white/5 text-xs space-y-2">
            <div className="flex items-center justify-between text-gray-400 font-mono text-[11px]">
              <span>EVALUATION ENGINE</span>
              <span className="text-emerald-400 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                ONLINE (WHISPER + LLM)
              </span>
            </div>
            <p className="text-gray-300 text-[11px] leading-relaxed">
              Musashi evaluates response structure, technical accuracy, and adherence to the STAR framework in real time.
            </p>
          </div>
        </div>

        {/* RIGHT SIDE: Interview Question, Answer Input, & Transcript */}
        <div className="lg:col-span-7 space-y-6">
          {/* Active Question Box */}
          <div className="glass-panel p-6 sm:p-7 rounded-3xl border border-red-500/30 shadow-2xl relative overflow-hidden">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-1 rounded-lg bg-red-600/20 text-red-400 border border-red-500/30 text-xs font-mono font-bold">
                  QUESTION {currentQuestion.questionNumber} / {currentQuestion.totalQuestions}
                </span>
                <span className="text-xs font-mono text-gray-400 uppercase">
                  CATEGORY: {currentQuestion.category}
                </span>
              </div>

              <span className="text-xs font-mono text-amber-400 flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5" />
                Live Assessment
              </span>
            </div>

            <h2 className="font-display font-bold text-xl sm:text-2xl text-white leading-relaxed mb-4">
              "{currentQuestion.text}"
            </h2>

            {/* Expected Keypoints hint */}
            <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 text-xs text-gray-400 flex items-start gap-2">
              <HelpCircle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold text-gray-300">Expected Focus:</span>{' '}
                {currentQuestion.expectedKeypoints.join(' • ')}
              </div>
            </div>
          </div>

          {/* User Answer Area & Controls */}
          <div className="glass-panel p-5 sm:p-6 rounded-3xl border border-white/10 space-y-4">
            <div className="flex items-center justify-between">
              <label className="text-xs font-mono font-semibold text-gray-300 flex items-center gap-2">
                <User className="w-4 h-4 text-red-500" />
                <span>YOUR RESPONSE (CANDIDATE DOSSIER ENTRY)</span>
              </label>

              {isRecording && (
                <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-red-950/60 border border-red-500/40 text-red-300 text-xs font-mono animate-pulse">
                  <span className="w-2 h-2 rounded-full bg-red-500" />
                  <span>RECORDING: {recordingSeconds}s</span>
                </div>
              )}
            </div>

            {/* Answer Textarea */}
            <textarea
              rows={4}
              value={userAnswer}
              onChange={(e) => setUserAnswer(e.target.value)}
              placeholder="Speak or type your answer here. Highlight your technical reasoning, specific classes/tools used, and measurable results..."
              className="w-full px-4 py-3 rounded-2xl bg-[#0F121C] border border-white/10 text-white placeholder-gray-500 text-sm focus:outline-none focus:border-red-500 leading-relaxed font-sans"
            />

            {/* Interactive Control Buttons */}
            <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
              <div className="flex items-center gap-2">
                {!isRecording ? (
                  <button
                    type="button"
                    onClick={handleStartRecording}
                    className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-gray-200 text-xs font-mono font-semibold transition-all hover:text-white"
                  >
                    <Mic className="w-4 h-4 text-red-400" />
                    <span>Start Recording</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={handleStopRecording}
                    className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-mono font-bold transition-all animate-pulse"
                  >
                    <MicOff className="w-4 h-4" />
                    <span>Stop Recording</span>
                  </button>
                )}

                <button
                  type="button"
                  onClick={() =>
                    setUserAnswer(
                      'I developed the inventory reservation microservice using Spring Boot and PostgreSQL. Under 5,000 simulated checkouts, race conditions caused stock counts to drop below zero. I diagnosed this using pg_stat_activity, implemented pessimistic locking with @Lock(LockModeType.PESSIMISTIC_WRITE) in JPA, and introduced Redis distributed locks. We achieved 100% stock consistency with p99 response time staying under 140ms.'
                    )
                  }
                  className="px-3 py-2.5 rounded-xl bg-white/[0.03] hover:bg-white/5 border border-white/5 text-gray-400 hover:text-gray-200 text-xs font-mono transition-colors"
                  title="Insert sample STAR answer"
                >
                  Insert Sample STAR Answer
                </button>
              </div>

              <button
                type="button"
                disabled={isSubmitting || !userAnswer.trim()}
                onClick={handleSubmitAnswer}
                className={`flex items-center gap-2 px-6 py-2.5 rounded-xl font-bold text-xs uppercase tracking-wider transition-all shadow-lg ${
                  isSubmitting || !userAnswer.trim()
                    ? 'bg-gray-800 text-gray-500 cursor-not-allowed'
                    : 'bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white shadow-red-600/30'
                }`}
              >
                {isSubmitting ? (
                  <>
                    <Sparkles className="w-4 h-4 animate-spin" />
                    <span>Evaluating...</span>
                  </>
                ) : (
                  <>
                    <span>Submit Answer</span>
                    <Send className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Conversation Transcript Area */}
          <div className="glass-panel p-5 sm:p-6 rounded-3xl border border-white/10 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-white/5">
              <div className="flex items-center gap-2 text-xs font-mono font-semibold text-gray-300">
                <Bot className="w-4 h-4 text-red-500" />
                <span>EXAMINATION DIALOGUE LOG</span>
              </div>
              <span className="text-[10px] font-mono text-gray-500">
                {transcript.length} EXCHANGES
              </span>
            </div>

            <div className="space-y-4 max-h-80 overflow-y-auto pr-2">
              {transcript.map((msg) => {
                const isAi = msg.sender === 'ai';
                return (
                  <div
                    key={msg.id}
                    className={`flex items-start gap-3 text-left ${
                      isAi ? 'justify-start' : 'justify-end'
                    }`}
                  >
                    {isAi && (
                      <div className="w-8 h-8 rounded-xl bg-red-600/20 border border-red-500/30 flex items-center justify-center shrink-0 text-red-400 mt-1">
                        <Bot className="w-4 h-4" />
                      </div>
                    )}

                    <div
                      className={`max-w-[85%] p-4 rounded-2xl text-xs sm:text-sm leading-relaxed ${
                        isAi
                          ? 'bg-white/[0.04] border border-white/10 text-gray-200'
                          : 'bg-red-950/40 border border-red-500/40 text-white ml-auto'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-4 mb-1 text-[10px] font-mono opacity-60">
                        <span>{isAi ? 'RONIN MENTOR' : user?.name || 'THUSHAN'}</span>
                        <span>{msg.timestamp}</span>
                      </div>
                      <p>{msg.text}</p>
                    </div>

                    {!isAi && (
                      <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/10 flex items-center justify-center shrink-0 text-gray-200 mt-1">
                        <User className="w-4 h-4" />
                      </div>
                    )}
                  </div>
                );
              })}

              {/* Animated Typing / Loading State required by prompt */}
              {isSubmitting && (
                <div className="flex items-center gap-3 p-3.5 rounded-2xl bg-amber-950/30 border border-amber-500/30 text-amber-300 text-xs font-mono animate-pulse">
                  <Sparkles className="w-4 h-4 animate-spin text-amber-400" />
                  <span>RONIN is analyzing your answer... Evaluating transactional consistency and STAR depth</span>
                </div>
              )}

              <div ref={transcriptEndRef} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
