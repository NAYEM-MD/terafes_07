import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Compass,
  Bot,
  BarChart3,
  FileCheck2,
  Settings,
  Sparkles,
  ChevronRight,
  ExternalLink,
  LogOut,
} from 'lucide-react';
import { MonEmblem } from '../common/MonEmblem';
import { KanjiBadge } from '../common/KanjiBadge';
import { useApp } from '../../context/AppContext';

interface SidebarProps {
  isOpen: boolean;
  onCloseMobile?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen, onCloseMobile }) => {
  const navigate = useNavigate();
  const { user, goalMode, isAccount, onboardingCompleted, signOut, addToast } = useApp();

  const navLinks = [
    {
      to: '/dashboard',
      label: 'Dashboard',
      sublabel: 'Command Center',
      icon: LayoutDashboard,
      badge: '92%',
    },
    {
      to: '/aimentor',
      label: 'AiMentor',
      sublabel: 'Job Opportunities',
      icon: Compass,
      badge: '24',
    },
    {
      to: '/mentor',
      label: 'AI Interview',
      sublabel: 'Live Interview',
      icon: Bot,
      highlight: true,
    },
    {
      to: '/analysis',
      label: 'Analysis',
      sublabel: 'Readiness & Fit',
      icon: BarChart3,
    },
    {
      to: '/report',
      label: 'Final Report',
      sublabel: '30-Day Action Roadmap',
      icon: FileCheck2,
    },
  ];

  return (
    <>
      {/* Mobile backdrop */}
      {isOpen && (
        <div
          onClick={onCloseMobile}
          className="fixed inset-0 bg-black/70 backdrop-blur-sm z-40 lg:hidden"
        />
      )}

      <aside
        className={`fixed top-0 left-0 bottom-0 z-40 w-72 bg-[#0C0E16]/95 border-r border-white/10 flex flex-col justify-between transition-transform duration-300 ease-in-out lg:translate-x-0 backdrop-blur-xl ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Top Branding Section */}
        <div className="flex flex-col">
          <div className="p-6 border-b border-white/5 flex items-center justify-between">
            <NavLink
              to="/"
              onClick={onCloseMobile}
              className="flex items-center gap-3.5 group"
            >
              <MonEmblem size={38} />
              <div className="flex flex-col">
                <div className="flex items-center gap-1.5">
                  <span className="font-display font-black text-xl tracking-wider text-white group-hover:text-red-400 transition-colors">
                    RONIN
                  </span>
                  <span className="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold bg-red-600/20 text-red-400 border border-red-500/30">
                    AI
                  </span>
                </div>
                <span className="text-[10px] text-gray-400 tracking-wider font-mono">
                  FORGE YOUR OWN PATH
                </span>
              </div>
            </NavLink>
          </div>

          {/* Current Path Mode Banner */}
          <div className="mx-4 mt-4 px-3 py-2 rounded-lg bg-white/[0.03] border border-white/5 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-base">{goalMode === 'job' ? '💼' : '🎓'}</span>
              <div className="text-left">
                <div className="text-[10px] font-mono text-gray-400 uppercase">
                  ACTIVE STREAM
                </div>
                <div className="text-xs font-semibold text-gray-200">
                  {goalMode === 'job' ? 'Career / Job Hunter' : 'University / Academia'}
                </div>
              </div>
            </div>
            <KanjiBadge kanji={goalMode === 'job' ? '就活' : '進学'} variant="gold" />
          </div>

          {/* Main Navigation */}
          <nav className="p-4 space-y-1.5">
            <div className="px-3 pb-2 text-[10px] font-mono uppercase tracking-widest text-gray-400">
              NAVIGATION PROTOCOLS
            </div>
            {navLinks.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={onCloseMobile}
                  className={({ isActive }) =>
                    `group relative flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${
                      isActive
                        ? 'bg-gradient-to-r from-red-600/20 to-red-950/10 text-white border border-red-500/40 shadow-lg shadow-red-950/30'
                        : 'text-gray-400 hover:text-white hover:bg-white/[0.04] border border-transparent'
                    }`
                  }
                >
                  {({ isActive }) => (
                    <>
                      <div className="flex items-center gap-3">
                        <div
                          className={`p-1.5 rounded-lg transition-colors ${
                            isActive
                              ? 'bg-red-600 text-white shadow-md shadow-red-600/30'
                              : item.highlight
                              ? 'bg-red-950/60 text-red-400 border border-red-500/30'
                              : 'bg-white/5 text-gray-400 group-hover:text-white'
                          }`}
                        >
                          <Icon className="w-4 h-4" />
                        </div>
                        <div className="text-left">
                          <div className="leading-tight">{item.label}</div>
                          <div className="text-[10px] text-gray-400 font-normal">
                            {item.sublabel}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1.5">
                        {item.badge && (
                          <span
                            className={`px-1.5 py-0.5 text-[10px] font-mono rounded ${
                              isActive
                                ? 'bg-red-500/30 text-white'
                                : 'bg-white/10 text-gray-300'
                            }`}
                          >
                            {item.badge}
                          </span>
                        )}
                        {item.highlight && !item.badge && (
                          <Sparkles className="w-3.5 h-3.5 text-red-400 animate-pulse" />
                        )}
                        <ChevronRight
                          className={`w-3.5 h-3.5 opacity-0 group-hover:opacity-100 transition-opacity ${
                            isActive ? 'opacity-100 text-red-400' : 'text-gray-500'
                          }`}
                        />
                      </div>
                    </>
                  )}
                </NavLink>
              );
            })}
          </nav>
        </div>

        {/* Bottom Section: Settings & User Identity */}
        <div className="p-4 border-t border-white/5 space-y-3">
          {!onboardingCompleted && (
            <NavLink
              to={isAccount ? '/onboarding' : '/login?intent=journey'}
              onClick={onCloseMobile}
              className="flex items-center justify-between px-3 py-2 rounded-lg text-xs text-gray-400 hover:text-white hover:bg-white/5 transition-colors border border-white/5"
            >
              <div className="flex items-center gap-2">
                <Settings className="w-4 h-4 text-gray-400" />
                <span>{isAccount ? 'Continue onboarding' : 'Start your journey'}</span>
              </div>
              <ExternalLink className="w-3.5 h-3.5 text-gray-400" />
            </NavLink>
          )}

          {/* User Profile Mini Card */}
          <NavLink
            to="/profile"
            onClick={onCloseMobile}
            aria-label="Open profile"
            className="flex items-center gap-3 p-2.5 rounded-xl bg-white/[0.02] hover:bg-white/[0.06] border border-white/5 transition-all group"
          >
            <div className="relative">
              <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-red-600 via-rose-700 to-amber-700 flex items-center justify-center font-display font-bold text-white text-base shadow-md">
                {user?.name?.charAt(0) || 'T'}
              </div>
              <div className="absolute -bottom-1 -right-1 w-3.5 h-3.5 rounded-full bg-emerald-500 border-2 border-[#0C0E16]" />
            </div>
            <div className="flex-1 text-left min-w-0">
              <div className="text-xs font-semibold text-white truncate group-hover:text-red-400 transition-colors">
                {user?.name || (isAccount ? 'Your profile' : 'Sample preview')}
              </div>
              <div className="text-[10px] font-mono text-gray-400 truncate">
                {isAccount ? user?.educationLevel || 'Account profile' : 'Sample preview'}
              </div>
            </div>
            <span className="text-[10px] font-mono font-bold text-emerald-400 bg-emerald-950/40 px-1.5 py-0.5 rounded border border-emerald-500/20">
              92%
            </span>
          </NavLink>
          {isAccount && (
            <button
              type="button"
              onClick={async () => {
                try {
                  await signOut();
                  onCloseMobile?.();
                  navigate('/');
                } catch (error) {
                  addToast(error instanceof Error ? error.message : 'Sign out failed', 'error');
                }
              }}
              className="w-full min-h-11 flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-xs text-gray-300 hover:text-white hover:bg-white/5 border border-white/10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
            >
              <LogOut className="w-4 h-4" />
              Sign out
            </button>
          )}
        </div>
      </aside>
    </>
  );
};
