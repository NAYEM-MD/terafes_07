import React from 'react';

interface KanjiBadgeProps {
  kanji: string;
  subtext?: string;
  variant?: 'crimson' | 'gold' | 'cyber';
  className?: string;
}

export const KanjiBadge: React.FC<KanjiBadgeProps> = ({
  kanji,
  subtext,
  variant = 'crimson',
  className = '',
}) => {
  const styles = {
    crimson: 'border-red-900/60 bg-red-950/20 text-red-400',
    gold: 'border-amber-700/60 bg-amber-950/20 text-amber-300',
    cyber: 'border-cyan-800/60 bg-cyan-950/20 text-cyan-400',
  };

  return (
    <div
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-sm border font-mono text-xs backdrop-blur-md ${styles[variant]} ${className}`}
    >
      <span className="font-japanese font-bold text-sm tracking-widest">{kanji}</span>
      {subtext && <span className="opacity-80 text-[11px] font-sans uppercase tracking-wider">{subtext}</span>}
    </div>
  );
};
