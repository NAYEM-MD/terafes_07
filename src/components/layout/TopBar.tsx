import React, { useState } from 'react';
import { useLocation, NavLink } from 'react-router-dom';
import { useApp } from '../../context/AppContext';
import {
  Bell,
  Menu,
  Sparkles,
  CheckCircle,
  Clock,
  X,
} from 'lucide-react';
import { KanjiBadge } from '../common/KanjiBadge';

interface TopBarProps {
  onToggleMobileSidebar: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({ onToggleMobileSidebar }) => {
  const location = useLocation();
  const { user } = useApp();
  const [showNotifications, setShowNotifications] = useState(false);

  // Map route path to readable title and Japanese badge
  const pageMetadata: Record<string, { title: string; kanji: string; sub: string }> = {
    '/dashboard': { title: 'DASHBOARD OVERVIEW', kanji: '司令部', sub: 'Command Center' },
    '/profile': { title: 'CANDIDATE DOSSIER', kanji: '武士録', sub: 'Verified Credentials' },
    '/aimentor': {
      title: 'AIMENTOR',
      kanji: '探索',
      sub: 'Job Opportunities',
    },
    '/mentor': { title: 'AI INTERVIEW', kanji: '道場', sub: 'Live Interview' },
    '/analysis': { title: 'READINESS ANALYSIS', kanji: '診断', sub: 'Vector Gap Breakdown' },
    '/report': { title: '30-DAY STRATEGY REPORT', kanji: '指南書', sub: 'Action Roadmap' },
    '/onboarding': { title: 'ONBOARDING PROTOCOL', kanji: '入門', sub: 'Profile Calibration' },
  };

  const currentMeta = pageMetadata[location.pathname] || {
    title: 'RONIN AI PLATFORM',
    kanji: '浪人',
    sub: 'Career Forge',
  };

  const notifications = [
    {
      id: 'notif-1',
      title: 'High Affinity Match Detected',
      message: 'TechNova Japan updated requirements. Your match score increased to 92%.',
      time: '12m ago',
      read: false,
    },
    {
      id: 'notif-2',
      title: 'Interview Analysis Ready',
      message: 'Your responses to the Spring Boot microservice inquiries have been evaluated.',
      time: '1h ago',
      read: false,
    },
    {
      id: 'notif-3',
      title: 'University Screening Reminder',
      message: 'Tsukuba Informatics Fall Intake Special Screening deadline approaches in 14 days.',
      time: '1d ago',
      read: true,
    },
  ];

  return (
    <header className="sticky top-0 z-30 h-16 w-full bg-[#0A0C12]/90 border-b border-white/10 backdrop-blur-xl px-4 lg:px-8 flex items-center justify-between">
      {/* Left: Mobile hamburger & Page Title */}
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleMobileSidebar}
          className="lg:hidden p-2 rounded-lg bg-white/5 hover:bg-white/10 text-gray-300 hover:text-white transition-colors"
          aria-label="Toggle menu"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-2.5">
          <KanjiBadge kanji={currentMeta.kanji} variant="crimson" className="hidden sm:inline-flex" />
          <div>
            <h1 className="text-sm font-bold text-white tracking-wider flex items-center gap-2 font-mono">
              {currentMeta.title}
            </h1>
            <p className="text-[10px] text-gray-400 font-sans hidden sm:block">
              {currentMeta.sub}
            </p>
          </div>
        </div>
      </div>

      {/* Right: Path Mode Switcher, Notifications, User Avatar */}
      <div className="flex items-center gap-3">
        {/* Readiness Pill */}
        <div className="hidden lg:flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-950/40 border border-emerald-500/30 text-emerald-300 text-xs font-mono">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>READINESS: 84%</span>
        </div>

        {/* Notifications Icon & Popover */}
        <div className="relative">
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative p-2 rounded-lg bg-white/5 hover:bg-white/10 text-gray-300 hover:text-white transition-colors"
            aria-label="Notifications"
          >
            <Bell className="w-4 h-4" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-red-500 shadow-sm shadow-red-500/80" />
          </button>

          {showNotifications && (
            <div className="absolute right-0 mt-3 w-80 sm:w-96 rounded-2xl bg-[#121522] border border-white/10 shadow-2xl backdrop-blur-2xl p-4 z-50">
              <div className="flex items-center justify-between pb-3 border-b border-white/5">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-red-400" />
                  <span className="text-xs font-bold font-mono text-white tracking-wider">
                    INTELLIGENCE DISPATCH
                  </span>
                </div>
                <button
                  onClick={() => setShowNotifications(false)}
                  className="text-gray-400 hover:text-white text-xs"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="mt-3 space-y-2 max-h-72 overflow-y-auto">
                {notifications.map((n) => (
                  <div
                    key={n.id}
                    className={`p-3 rounded-xl border text-left transition-colors ${
                      n.read
                        ? 'bg-white/[0.02] border-white/5 text-gray-400'
                        : 'bg-red-950/20 border-red-500/20 text-gray-200'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-semibold text-white">{n.title}</span>
                      <span className="text-[10px] text-gray-400 flex items-center gap-1 font-mono">
                        <Clock className="w-3 h-3" />
                        {n.time}
                      </span>
                    </div>
                    <p className="text-xs text-gray-300 mt-1 leading-relaxed">
                      {n.message}
                    </p>
                  </div>
                ))}
              </div>

              <div className="mt-3 pt-2 border-t border-white/5 text-center">
                <button
                  onClick={() => setShowNotifications(false)}
                  className="text-[11px] font-mono text-red-400 hover:text-red-300 flex items-center justify-center gap-1.5 w-full"
                >
                  <CheckCircle className="w-3 h-3" /> Mark all intelligence read
                </button>
              </div>
            </div>
          )}
        </div>

        {/* User Mini Avatar */}
        <NavLink
          to="/profile"
          className="flex items-center gap-2.5 p-1 pr-2.5 rounded-full bg-white/5 hover:bg-white/10 border border-white/5 transition-all group"
        >
          <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-red-600 to-amber-600 flex items-center justify-center text-xs font-bold text-white shadow-sm">
            {user?.name?.charAt(0) || 'T'}
          </div>
          <span className="text-xs font-medium text-gray-200 hidden sm:inline-block group-hover:text-red-400">
            {user?.name || 'Thushan'}
          </span>
        </NavLink>
      </div>
    </header>
  );
};
