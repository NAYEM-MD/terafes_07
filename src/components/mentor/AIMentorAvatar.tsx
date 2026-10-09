import React from 'react';
import { MentorAvatarState } from '../../types/interview';
import { MonEmblem } from '../common/MonEmblem';
import { Sparkles, Activity, Brain, Volume2, Mic } from 'lucide-react';

interface AIMentorAvatarProps {
  state: MentorAvatarState;
  size?: 'md' | 'lg' | 'hero';
  className?: string;
  onStateChange?: (state: MentorAvatarState) => void;
  showControls?: boolean;
}

export const AIMentorAvatar: React.FC<AIMentorAvatarProps> = ({
  state = 'idle',
  size = 'lg',
  className = '',
  onStateChange,
  showControls = false,
}) => {
  // Dimension mappings
  const dimensions = {
    md: 'w-64 h-80',
    lg: 'w-full max-w-[420px] h-[480px]',
    hero: 'w-full max-w-[460px] h-[520px]',
  }[size];

  // Visual cues based on state
  const stateConfig = {
    idle: {
      label: 'Mentor Standby',
      color: 'border-white/10 shadow-black/60',
      glow: 'from-slate-800/10 via-transparent to-transparent',
      statusBg: 'bg-slate-900/80 border-slate-700/60 text-slate-300',
      icon: <Sparkles className="w-3.5 h-3.5 text-slate-400" />,
      ringColor: 'stroke-white/10',
      visorGlow: 'text-gray-400 shadow-gray-500/20',
      activeWave: false,
    },
    listening: {
      label: 'AI Listening',
      color: 'border-cyan-500/40 shadow-cyan-950/40',
      glow: 'from-cyan-950/30 via-slate-900/10 to-transparent',
      statusBg: 'bg-cyan-950/90 border-cyan-500/50 text-cyan-300',
      icon: <Mic className="w-3.5 h-3.5 text-cyan-400 animate-pulse" />,
      ringColor: 'stroke-cyan-500/60',
      visorGlow: 'text-cyan-400 shadow-cyan-400/50',
      activeWave: true,
    },
    thinking: {
      label: 'AI Thinking',
      color: 'border-amber-500/40 shadow-amber-950/40',
      glow: 'from-amber-950/30 via-slate-900/10 to-transparent',
      statusBg: 'bg-amber-950/90 border-amber-500/50 text-amber-300',
      icon: <Brain className="w-3.5 h-3.5 text-amber-400 animate-spin" />,
      ringColor: 'stroke-amber-500/60',
      visorGlow: 'text-amber-400 shadow-amber-400/50',
      activeWave: false,
    },
    speaking: {
      label: 'AI Speaking',
      color: 'border-red-500/50 shadow-red-950/50',
      glow: 'from-red-950/30 via-slate-900/10 to-transparent',
      statusBg: 'bg-red-950/90 border-red-500/60 text-red-300',
      icon: <Volume2 className="w-3.5 h-3.5 text-red-400 animate-bounce" />,
      ringColor: 'stroke-red-500/70',
      visorGlow: 'text-red-400 shadow-red-500/50',
      activeWave: true,
    },
  }[state];

  return (
    <div className={`relative flex flex-col items-center select-none ${className}`}>
      {/* Outer Holographic Container Frame */}
      <div
        className={`relative ${dimensions} rounded-2xl border backdrop-blur-xl bg-gradient-to-b from-[#121522]/95 via-[#0D0F18]/95 to-[#08090E]/95 overflow-hidden flex flex-col items-center justify-between p-6 transition-all duration-700 ${stateConfig.color}`}
        style={{
          boxShadow:
            state === 'speaking'
              ? '0 0 50px -10px rgba(229, 57, 53, 0.4), inset 0 0 30px rgba(229, 57, 53, 0.1)'
              : state === 'listening'
              ? '0 0 50px -10px rgba(0, 229, 255, 0.35), inset 0 0 30px rgba(0, 229, 255, 0.1)'
              : state === 'thinking'
              ? '0 0 50px -10px rgba(212, 175, 55, 0.35), inset 0 0 30px rgba(212, 175, 55, 0.1)'
              : '0 20px 40px -15px rgba(0, 0, 0, 0.8)',
        }}
      >
        {/* Subtle Cyberpunk HUD Grid & Corner Brackets */}
        <div className="absolute inset-0 cyber-grid opacity-30 pointer-events-none" />
        <div className="absolute top-2 left-2 w-4 h-4 border-t-2 border-l-2 border-red-500/60 pointer-events-none" />
        <div className="absolute top-2 right-2 w-4 h-4 border-t-2 border-r-2 border-red-500/60 pointer-events-none" />
        <div className="absolute bottom-2 left-2 w-4 h-4 border-b-2 border-l-2 border-red-500/60 pointer-events-none" />
        <div className="absolute bottom-2 right-2 w-4 h-4 border-b-2 border-r-2 border-red-500/60 pointer-events-none" />

        {/* Ambient background glow ring */}
        <div
          className={`absolute inset-0 bg-gradient-to-t ${stateConfig.glow} pointer-events-none transition-all duration-700`}
        />

        {/* Top Header Bar inside hologram */}
        <div className="relative z-20 w-full flex items-center justify-between">
          <div className="flex items-center gap-2">
            <MonEmblem size={24} hasGlow={false} />
            <span className="font-mono text-[11px] tracking-wider text-gray-400 uppercase">
              RONIN AI MENTOR
            </span>
          </div>

          {/* Status Indicator Pill */}
          <div
            className={`flex items-center gap-1.5 px-3 py-1 rounded-full border text-xs font-mono font-medium backdrop-blur-md shadow-lg transition-all duration-300 ${stateConfig.statusBg}`}
          >
            {stateConfig.icon}
            <span>{stateConfig.label}</span>
          </div>
        </div>

        {/* Central Futuristic Samurai Avatar Canvas (Placeholder for LivePortrait/MuseTalk) */}
        <div className="relative z-10 my-auto flex flex-col items-center justify-center">
          {/* Animated Orbital Energy Rings */}
          <div className="relative w-56 h-56 flex items-center justify-center">
            {/* Outer rotating dashed ring */}
            <svg
              className={`absolute inset-0 w-full h-full transform transition-all duration-1000 ${
                state === 'thinking' ? 'animate-[spin_4s_linear_infinite]' : 'animate-[spin_20s_linear_infinite]'
              }`}
              viewBox="0 0 100 100"
            >
              <circle
                cx="50"
                cy="50"
                r="46"
                fill="none"
                strokeWidth="1.5"
                strokeDasharray="6 4 2 4"
                className={stateConfig.ringColor}
              />
            </svg>

            {/* Inner pulsating glow disc */}
            <div
              className={`absolute w-44 h-44 rounded-full transition-all duration-700 ${
                state === 'speaking'
                  ? 'bg-red-600/15 blur-xl scale-110'
                  : state === 'listening'
                  ? 'bg-cyan-500/15 blur-xl scale-105'
                  : state === 'thinking'
                  ? 'bg-amber-500/15 blur-xl scale-105'
                  : 'bg-white/5 blur-lg'
              }`}
            />

            {/* Avatar Holographic Portrait Visual Placeholder */}
            <div className="relative w-44 h-44 rounded-full border border-white/15 bg-gradient-to-b from-[#181C2A] to-[#0A0B10] flex items-center justify-center overflow-hidden shadow-inner group">
              {/* Scanline overlay */}
              <div
                className="absolute inset-0 opacity-20 pointer-events-none"
                style={{
                  backgroundImage:
                    'repeating-linear-gradient(0deg, transparent, transparent 2px, rgba(255, 255, 255, 0.05) 3px)',
                }}
              />

              {/* Holographic Samurai Mentor Silhouette & Visor Graphics */}
              <svg
                viewBox="0 0 120 120"
                className="w-36 h-36 relative z-10 transition-transform duration-500 group-hover:scale-105"
                fill="none"
              >
                {/* Shoulders & Traditional Haori / Cyber Robe */}
                <path
                  d="M15 110C25 88 40 82 60 82C80 82 95 88 105 110"
                  stroke="#334155"
                  strokeWidth="3.5"
                  strokeLinecap="round"
                />
                <path
                  d="M32 94L48 118M88 94L72 118"
                  stroke="#E53935"
                  strokeWidth="2"
                  strokeOpacity="0.8"
                />

                {/* Neck & Chin */}
                <path
                  d="M52 74V82H68V74"
                  stroke="#475569"
                  strokeWidth="3"
                />
                {/* Cybernetic Face / Helmet Mask Base */}
                <path
                  d="M40 42C40 30 50 20 60 20C70 20 80 30 80 42C80 58 72 74 60 76C48 74 40 58 40 42Z"
                  fill="#0E121E"
                  stroke="#475569"
                  strokeWidth="2"
                />

                {/* Samurai Kabuto Crest / Maedate (Forehead Horn Ornament) */}
                <path
                  d="M60 12L54 22H66L60 12Z"
                  fill="#D4AF37"
                />
                <path
                  d="M48 20L60 8L72 20"
                  stroke="#D4AF37"
                  strokeWidth="2.5"
                  strokeLinecap="round"
                />

                {/* Cybernetic HUD Visor (The glowing focal point) */}
                <path
                  d="M45 44H75L72 52H48L45 44Z"
                  fill="#111827"
                  stroke={
                    state === 'speaking'
                      ? '#E53935'
                      : state === 'listening'
                      ? '#00E5FF'
                      : state === 'thinking'
                      ? '#D4AF37'
                      : '#64748B'
                  }
                  strokeWidth="2"
                  style={{
                    filter: `drop-shadow(0 0 6px ${
                      state === 'speaking'
                        ? '#E53935'
                        : state === 'listening'
                        ? '#00E5FF'
                        : state === 'thinking'
                        ? '#D4AF37'
                        : 'transparent'
                    })`,
                  }}
                />

                {/* Dual glowing optical sensor lines */}
                <circle
                  cx="53"
                  cy="48"
                  r="2.5"
                  fill={
                    state === 'speaking'
                      ? '#FF5252'
                      : state === 'listening'
                      ? '#00E5FF'
                      : state === 'thinking'
                      ? '#F59E0B'
                      : '#94A3B8'
                  }
                />
                <circle
                  cx="67"
                  cy="48"
                  r="2.5"
                  fill={
                    state === 'speaking'
                      ? '#FF5252'
                      : state === 'listening'
                      ? '#00E5FF'
                      : state === 'thinking'
                      ? '#F59E0B'
                      : '#94A3B8'
                  }
                />

                {/* Subtle Japanese Mon watermark on chest */}
                <circle cx="60" cy="98" r="8" stroke="#D4AF37" strokeWidth="1" strokeOpacity="0.6" />
                <polygon points="60,93 64,98 60,103 56,98" fill="#E53935" fillOpacity="0.8" />
              </svg>

              {/* Neural mesh nodes in background */}
              <div className="absolute bottom-2 text-[9px] font-mono text-gray-500 uppercase tracking-widest">
                KAZENIN V3.4 • NEURAL SENSE
              </div>
            </div>
          </div>

          <div className="mt-3 text-center">
            <div className="text-xs font-semibold text-gray-200 tracking-wider">
              MENTOR MUSASHI
            </div>
            <div className="text-[10px] font-mono text-gray-500">
              Adaptive Japanese Career & Technical Examiner
            </div>
          </div>
        </div>

        {/* Bottom Audio Waveform Visualizer */}
        <div className="relative z-20 w-full flex flex-col items-center">
          <div className="flex items-center justify-center gap-1.5 h-9 w-full max-w-[280px] px-3 py-1 rounded-lg bg-black/40 border border-white/5">
            {[18, 28, 12, 34, 46, 22, 54, 38, 24, 48, 16, 32, 20, 42, 14].map((height, i) => {
              const isWaveActive = state === 'speaking' || state === 'listening';
              const waveHeight = isWaveActive ? height : 6;
              const barColor =
                state === 'speaking'
                  ? 'bg-red-500 shadow-red-500/50'
                  : state === 'listening'
                  ? 'bg-cyan-400 shadow-cyan-400/50'
                  : state === 'thinking'
                  ? 'bg-amber-400/60'
                  : 'bg-gray-600/50';

              return (
                <div
                  key={i}
                  className={`w-1 rounded-full transition-all duration-200 ${barColor}`}
                  style={{
                    height: `${waveHeight}px`,
                    animationDelay: `${(i % 5) * 0.15}s`,
                    animationDuration: state === 'speaking' ? '0.8s' : '1.4s',
                  }}
                />
              );
            })}
          </div>

          <div className="mt-2 text-[10px] font-mono text-gray-500 flex items-center gap-2">
            <Activity className="w-3 h-3 text-red-500/80" />
            <span>LATENCY: 42ms • MODEL: RONIN-REASONER-J1</span>
          </div>
        </div>
      </div>

      {/* Optional Debug / Interactive State Selector (for manual demonstration/presentation) */}
      {showControls && (
        <div className="mt-4 flex items-center gap-1.5 p-1.5 rounded-lg bg-[#141722] border border-white/10 text-xs font-mono">
          <span className="text-gray-400 px-2 text-[11px]">DEMO STATE:</span>
          {(['idle', 'listening', 'thinking', 'speaking'] as MentorAvatarState[]).map((s) => (
            <button
              key={s}
              onClick={() => onStateChange && onStateChange(s)}
              className={`px-2.5 py-1 rounded transition-colors uppercase text-[10px] ${
                state === s
                  ? 'bg-red-600 text-white font-bold'
                  : 'text-gray-400 hover:text-white hover:bg-white/5'
              }`}
            >
              {s}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
