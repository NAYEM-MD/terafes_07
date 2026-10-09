import React from 'react';

interface ScoreGaugeProps {
  score: number;
  size?: number;
  strokeWidth?: number;
  label?: string;
  sublabel?: string;
  showPercent?: boolean;
  className?: string;
}

export const ScoreGauge: React.FC<ScoreGaugeProps> = ({
  score,
  size = 140,
  strokeWidth = 10,
  label,
  sublabel,
  showPercent = true,
  className = '',
}) => {
  const radius = (size - strokeWidth * 2) / 2;
  const circumference = 2 * Math.PI * radius;
  const clampedScore = Math.min(Math.max(score, 0), 100);
  const strokeDashoffset = circumference - (clampedScore / 100) * circumference;

  // Determine color based on score
  let strokeColor = '#E53935'; // default crimson
  let glowColor = 'rgba(229, 57, 53, 0.4)';
  if (clampedScore >= 90) {
    strokeColor = '#10B981'; // emerald green / top match
    glowColor = 'rgba(16, 185, 129, 0.4)';
  } else if (clampedScore >= 80) {
    strokeColor = '#D4AF37'; // gold
    glowColor = 'rgba(212, 175, 55, 0.4)';
  } else if (clampedScore >= 70) {
    strokeColor = '#3B82F6'; // cyber blue
    glowColor = 'rgba(59, 130, 246, 0.4)';
  }

  return (
    <div className={`relative inline-flex flex-col items-center justify-center ${className}`}>
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="transform -rotate-90">
          {/* Background circle track */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke="rgba(255, 255, 255, 0.08)"
            strokeWidth={strokeWidth}
            fill="transparent"
          />
          {/* Progress circle */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            stroke={strokeColor}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            fill="transparent"
            style={{
              transition: 'stroke-dashoffset 1.2s cubic-bezier(0.16, 1, 0.3, 1)',
              filter: `drop-shadow(0 0 8px ${glowColor})`,
            }}
          />
        </svg>

        {/* Center content */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
          <span className="font-display font-bold text-3xl text-white tracking-tight">
            {clampedScore}
            {showPercent && <span className="text-lg text-gray-400 font-sans font-normal">%</span>}
          </span>
          {sublabel && (
            <span className="text-[10px] font-mono uppercase tracking-wider text-gray-400">
              {sublabel}
            </span>
          )}
        </div>
      </div>

      {label && (
        <span className="mt-2 text-xs font-medium text-gray-300 tracking-wide text-center">
          {label}
        </span>
      )}
    </div>
  );
};
