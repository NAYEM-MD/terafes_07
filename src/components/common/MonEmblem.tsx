import React from 'react';

interface MonEmblemProps {
  size?: number;
  className?: string;
  hasGlow?: boolean;
}

export const MonEmblem: React.FC<MonEmblemProps> = ({
  size = 36,
  className = '',
  hasGlow = true,
}) => {
  return (
    <div
      className={`relative inline-flex items-center justify-center ${className}`}
      style={{ width: size, height: size }}
    >
      {hasGlow && (
        <div
          className="absolute inset-0 rounded-full bg-red-600/30 blur-md pointer-events-none animate-pulse-slow"
          style={{ width: size, height: size }}
        />
      )}
      <svg
        width={size}
        height={size}
        viewBox="0 0 100 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="relative z-10"
      >
        {/* Outer Samurai Circle */}
        <circle
          cx="50"
          cy="50"
          r="46"
          stroke="#E53935"
          strokeWidth="3.5"
          strokeDasharray="4 2"
          className="opacity-90"
        />
        {/* Inner Ring */}
        <circle
          cx="50"
          cy="50"
          r="40"
          stroke="#D4AF37"
          strokeWidth="1.5"
          className="opacity-70"
        />

        {/* Crossed Katana Blade Angles */}
        <path
          d="M26 74L74 26"
          stroke="#FFFFFF"
          strokeWidth="2.5"
          strokeLinecap="round"
        />
        <path
          d="M74 74L26 26"
          stroke="#FFFFFF"
          strokeWidth="2.5"
          strokeLinecap="round"
        />

        {/* Central Geometric Diamond Mon */}
        <polygon
          points="50,22 78,50 50,78 22,50"
          fill="#141722"
          stroke="#E53935"
          strokeWidth="2"
        />

        {/* Golden Sun / AI Core */}
        <circle cx="50" cy="50" r="8" fill="#D4AF37" />
        <circle cx="50" cy="50" r="4" fill="#FFFFFF" />

        {/* Cyberpunk corner notches */}
        <line x1="50" y1="4" x2="50" y2="10" stroke="#E53935" strokeWidth="2.5" />
        <line x1="50" y1="90" x2="50" y2="96" stroke="#E53935" strokeWidth="2.5" />
        <line x1="4" y1="50" x2="10" y2="50" stroke="#E53935" strokeWidth="2.5" />
        <line x1="90" y1="50" x2="96" y2="50" stroke="#E53935" strokeWidth="2.5" />
      </svg>
    </div>
  );
};
