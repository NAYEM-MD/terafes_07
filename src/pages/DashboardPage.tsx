import React, { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { roninApi } from '../services/api';
import { Opportunity } from '../types/opportunity';
import {
  Sparkles,
  TrendingUp,
  Briefcase,
  GraduationCap,
  CheckCircle2,
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Calendar,
  ChevronRight,
  Bookmark,
  Bot,
  Layers,
  Award,
} from 'lucide-react';
import { ScoreGauge } from '../components/common/ScoreGauge';
import { KanjiBadge } from '../components/common/KanjiBadge';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const {
    user,
    goalMode,
    setSelectedOpportunityId,
    toggleSaveOpportunity,
    savedOpportunityIds,
    authReady,
    authUser,
    onboardingCompleted,
    isLoadingUser,
    isAccount,
  } = useApp();
  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    if (!authReady || isLoadingUser) return;
    if (authUser && !onboardingCompleted) {
      navigate('/onboarding', { replace: true });
    }
  }, [authReady, isLoadingUser, authUser, onboardingCompleted, navigate]);

  useEffect(() => {
    async function loadData() {
      setLoading(true);
      const data = await roninApi.searchOpportunities({ type: goalMode });
      setOpportunities(data);
      setLoading(false);
    }
    loadData();
  }, [goalMode]);

  // Today's Top Recommended Path
  const recommendedOpportunity = opportunities[0];

  const handleSelectOpportunity = (id: string) => {
    setSelectedOpportunityId(id);
    navigate(`/opportunity/${id}`);
  };

  if (!authReady || isLoadingUser || (authUser && !onboardingCompleted)) {
    return (
      <div className="py-20 text-center font-mono text-gray-400">
        <Sparkles className="w-8 h-8 text-red-500 animate-spin mx-auto mb-3" />
        <div>CHECKING YOUR ACCOUNT...</div>
      </div>
    );
  }

  return (
    <div className="space-y-8 text-left">
      {/* Top Welcome Banner with Samurai Aesthetic */}
      <div className="relative glass-panel rounded-3xl p-6 sm:p-8 border border-white/10 overflow-hidden">
        {/* Subtle decorative katana slash */}
        <div className="absolute top-0 right-0 w-80 h-full bg-gradient-to-l from-red-600/10 via-transparent to-transparent pointer-events-none" />
        <div className="absolute -bottom-8 -right-8 w-44 h-44 rounded-full bg-red-600/10 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col gap-6">
          <NavLink
            to="/"
            className="inline-flex w-fit items-center gap-2 px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-gray-300 hover:text-white text-xs font-mono border border-white/10 transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Home</span>
          </NavLink>
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <KanjiBadge kanji="開眼" subtext="Awakening" variant="crimson" />
              <span className="text-xs font-mono text-gray-400">
                ACTIVE COHORT • 2027 NEW GRADUATE INTAKE
              </span>
            </div>
            <h1 className="font-display font-black text-3xl sm:text-4xl text-white tracking-tight">
              Welcome back, <span className="text-red-500">{user?.name || 'there'}</span>.
            </h1>
            <p className="text-sm text-gray-300 font-sans max-w-xl">
              "Your path is becoming clearer." The tactical analysis matrix has evaluated 24 target pipelines
              in Tokyo matching your technical stack and bilingual qualifications.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <NavLink
              to="/mentor"
              className="flex items-center gap-2 px-5 py-3 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-lg shadow-red-600/30"
            >
              <Bot className="w-4 h-4" />
              <span>Launch AI Interview</span>
            </NavLink>
            <NavLink
              to="/aimentor"
              className="flex items-center gap-2 px-4 py-3 rounded-xl bg-white/5 hover:bg-white/10 text-gray-200 text-xs font-semibold border border-white/10 transition-colors font-mono"
            >
              <span>Open AiMentor</span>
              <ChevronRight className="w-4 h-4" />
            </NavLink>
          </div>
        </div>
        </div>
      </div>

      <section aria-label="Profile summary" className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/10">
        <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <KanjiBadge kanji="武士録" subtext={isAccount ? 'Your Dossier' : 'Sample Dossier'} variant="crimson" />
              <span className="text-xs font-mono text-gray-400">
                {isAccount ? 'ACCOUNT PROFILE' : 'SAMPLE PROFILE'}
              </span>
            </div>
            <h2 className="font-display font-bold text-2xl text-white">
              {user?.name || 'Profile not saved yet'}
            </h2>
            <p className="text-sm text-gray-300">
              {[user?.currentSchool, user?.major, user?.currentLocation].filter(Boolean).join(' · ') ||
                'School, major, and location appear here after onboarding.'}
            </p>
            <div className="flex flex-wrap gap-2 pt-1">
              {(user?.skills || []).slice(0, 4).map((skill) => (
                <span
                  key={skill.id}
                  className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/10 text-xs text-gray-200"
                >
                  {skill.name}
                </span>
              ))}
            </div>
            {!isAccount && (
              <p className="text-xs text-amber-200/90">
                This is the sample dossier. Start Your Journey to save a profile to your own account.
              </p>
            )}
          </div>
          <NavLink
            to="/profile"
            className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white text-xs font-bold uppercase tracking-wider shadow-lg shadow-red-600/30 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
          >
            View and edit profile
            <ArrowRight className="w-4 h-4" />
          </NavLink>
        </div>
      </section>

      {/* 4 KPI Metrics Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <div className="glass-panel p-5 rounded-2xl border border-white/10 relative overflow-hidden group hover:border-red-500/40 transition-all">
          <div className="flex items-center justify-between text-gray-400 text-xs font-mono mb-2">
            <span>PROFILE COMPLETION</span>
            <span className="font-japanese text-sm text-gray-500 group-hover:text-red-400 transition-colors">
              完成度
            </span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="font-display font-bold text-3xl sm:text-4xl text-white">
              {user?.profileCompletion || 92}%
            </span>
            <span className="text-xs text-emerald-400 font-mono">+4% this week</span>
          </div>
          <div className="mt-3 w-full bg-white/10 h-1.5 rounded-full overflow-hidden">
            <div
              className="bg-red-500 h-full rounded-full transition-all duration-1000"
              style={{ width: `${user?.profileCompletion || 92}%` }}
            />
          </div>
        </div>

        <div className="glass-panel p-5 rounded-2xl border border-white/10 relative overflow-hidden group hover:border-amber-500/40 transition-all">
          <div className="flex items-center justify-between text-gray-400 text-xs font-mono mb-2">
            <span>OPPORTUNITIES FOUND</span>
            <span className="font-japanese text-sm text-gray-500 group-hover:text-amber-400 transition-colors">
              求人網
            </span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="font-display font-bold text-3xl sm:text-4xl text-white">24</span>
            <span className="text-xs text-gray-400 font-mono">in Tokyo Area</span>
          </div>
          <div className="mt-3 flex items-center gap-1.5 text-[11px] text-amber-400 font-mono">
            <TrendingUp className="w-3.5 h-3.5" />
            <span>3 new posted today</span>
          </div>
        </div>

        <div className="glass-panel p-5 rounded-2xl border border-white/10 relative overflow-hidden group hover:border-emerald-500/40 transition-all">
          <div className="flex items-center justify-between text-gray-400 text-xs font-mono mb-2">
            <span>STRONG MATCHES</span>
            <span className="font-japanese text-sm text-gray-500 group-hover:text-emerald-400 transition-colors">
              適合度
            </span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="font-display font-bold text-3xl sm:text-4xl text-emerald-400">8</span>
            <span className="text-xs text-emerald-300 font-mono">&gt; 85% Affinity</span>
          </div>
          <div className="mt-3 text-[11px] text-gray-400 font-mono flex items-center gap-1">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            <span>Top tier placement chances</span>
          </div>
        </div>

        <div className="glass-panel p-5 rounded-2xl border border-white/10 relative overflow-hidden group hover:border-cyan-500/40 transition-all">
          <div className="flex items-center justify-between text-gray-400 text-xs font-mono mb-2">
            <span>INTERVIEW READINESS</span>
            <span className="font-japanese text-sm text-gray-500 group-hover:text-cyan-400 transition-colors">
              練度
            </span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="font-display font-bold text-3xl sm:text-4xl text-cyan-400">74%</span>
            <span className="text-xs text-gray-400 font-mono">Target: 85%</span>
          </div>
          <div className="mt-3 text-[11px] text-gray-400 font-mono flex items-center gap-1">
            <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
            <span>STAR method calibration next</span>
          </div>
        </div>
      </div>

      {/* Main Grid: Today's Recommended Path + Skill Progress */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column (8 cols): Today's Recommended Path Feature Card */}
        <div className="lg:col-span-8 space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <KanjiBadge kanji="本日の推奨" subtext="Top Priority" variant="crimson" />
              <h2 className="font-display font-bold text-xl text-white">
                Today's Recommended Path
              </h2>
            </div>
            <span className="text-xs font-mono text-gray-400">
              UPDATED 09:30 JST
            </span>
          </div>

          {recommendedOpportunity && (
            <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-red-500/30 shadow-2xl relative overflow-hidden">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 pb-6 border-b border-white/10">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-mono text-red-400 font-semibold uppercase">
                      {recommendedOpportunity.organization}
                    </span>
                    <span className="w-1 h-1 rounded-full bg-gray-500" />
                    <span className="text-xs text-gray-400 font-mono">
                      {recommendedOpportunity.location}
                    </span>
                  </div>
                  <h3 className="font-display font-black text-2xl text-white">
                    {recommendedOpportunity.title}
                  </h3>
                  <p className="text-xs text-gray-300 font-mono">
                    {recommendedOpportunity.salaryOrTuition} • {recommendedOpportunity.workStyleOrProgramType}
                  </p>
                </div>

                <ScoreGauge
                  score={recommendedOpportunity.matchScore}
                  size={120}
                  strokeWidth={9}
                  label="AFFINITY FIT"
                />
              </div>

              {/* Match Factors Breakdown */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 py-6 border-b border-white/10">
                {/* Why it matches */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-xs font-mono font-bold text-emerald-400 uppercase">
                    <CheckCircle2 className="w-4 h-4" />
                    <span>Why It Matches:</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {['Java', 'Spring Boot', 'Database (PostgreSQL)', 'JLPT N2', 'Linux Admin'].map((tag) => (
                      <span
                        key={tag}
                        className="px-2.5 py-1 rounded-lg bg-emerald-950/40 border border-emerald-500/30 text-emerald-300 text-xs font-medium"
                      >
                        ✅ {tag}
                      </span>
                    ))}
                  </div>
                  <p className="text-xs text-gray-400 leading-relaxed">
                    Verified through your Oracle Certified Professional credential and two backend microservice capstones.
                  </p>
                </div>

                {/* Missing / Gaps */}
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-xs font-mono font-bold text-amber-400 uppercase">
                    <AlertTriangle className="w-4 h-4" />
                    <span>Missing / Growth Area:</span>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {['AWS Production', 'Commercial Production Experience'].map((tag) => (
                      <span
                        key={tag}
                        className="px-2.5 py-1 rounded-lg bg-amber-950/40 border border-amber-500/30 text-amber-300 text-xs font-medium"
                      >
                        ⚠️ {tag}
                      </span>
                    ))}
                  </div>
                  <p className="text-xs text-gray-400 leading-relaxed">
                    Can be overcome in Week 1 of your Action Plan via our containerized AWS ECS deployment blueprint.
                  </p>
                </div>
              </div>

              {/* Card Footer CTAs */}
              <div className="pt-6 flex flex-wrap items-center justify-between gap-4">
                <div className="text-xs font-mono text-gray-400 flex items-center gap-2">
                  <Layers className="w-4 h-4 text-gray-500" />
                  <span>Based on published criteria: 5 of 6 criteria satisfied</span>
                </div>

                <div className="flex items-center gap-3">
                  <button
                    onClick={() => toggleSaveOpportunity(recommendedOpportunity.id)}
                    className="p-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-gray-300 hover:text-white border border-white/10 transition-colors"
                    title="Bookmark opportunity"
                  >
                    <Bookmark
                      className={`w-4 h-4 ${
                        savedOpportunityIds.includes(recommendedOpportunity.id)
                          ? 'fill-amber-400 text-amber-400'
                          : ''
                      }`}
                    />
                  </button>

                  <button
                    onClick={() => handleSelectOpportunity(recommendedOpportunity.id)}
                    className="flex items-center gap-2 px-6 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-lg shadow-red-600/30"
                  >
                    <span>View Analysis</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Recent Opportunities List */}
          <div className="space-y-4 pt-4">
            <div className="flex items-center justify-between">
              <h3 className="font-display font-bold text-lg text-white">
                Recent Opportunities ({opportunities.length})
              </h3>
              <NavLink
                to="/aimentor"
                className="text-xs font-mono text-red-400 hover:text-red-300 flex items-center gap-1"
              >
                <span>Open AiMentor</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </NavLink>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {opportunities.slice(1, 5).map((opp) => (
                <div
                  key={opp.id}
                  onClick={() => handleSelectOpportunity(opp.id)}
                  className="glass-panel glass-panel-hover p-5 rounded-2xl cursor-pointer group flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-[10px] font-mono text-gray-400 uppercase">
                        {opp.organization}
                      </span>
                      <span
                        className={`text-xs font-mono font-bold px-2 py-0.5 rounded ${
                          opp.matchScore >= 90
                            ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-500/30'
                            : opp.matchScore >= 80
                            ? 'bg-amber-950/60 text-amber-400 border border-amber-500/30'
                            : 'bg-blue-950/60 text-blue-400 border border-blue-500/30'
                        }`}
                      >
                        {opp.matchScore}% MATCH
                      </span>
                    </div>
                    <h4 className="font-bold text-white text-base group-hover:text-red-400 transition-colors">
                      {opp.title}
                    </h4>
                    <p className="text-xs text-gray-400 mt-1 line-clamp-2">
                      {opp.description}
                    </p>
                  </div>

                  <div className="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-xs font-mono text-gray-400">
                    <span>{opp.location}</span>
                    <span className="text-red-400 flex items-center gap-1 group-hover:translate-x-1 transition-transform">
                      Analyze →
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column (4 cols): Skill Progress & Upcoming Tasks */}
        <div className="lg:col-span-4 space-y-6">
          {/* Skill Mastery Progress */}
          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Award className="w-4 h-4 text-amber-400" />
                <h3 className="font-display font-bold text-base text-white">
                  Skill Mastery Progress
                </h3>
              </div>
              <KanjiBadge kanji="練磨" variant="gold" />
            </div>

            <div className="space-y-3.5">
              {[
                { name: 'Core Java (SE 17 / JVM)', level: 94, status: 'Mastered' },
                { name: 'Spring Boot Architecture', level: 86, status: 'Advanced' },
                { name: 'Relational DB / SQL', level: 88, status: 'Advanced' },
                { name: 'Japanese (JLPT N2)', level: 82, status: 'Fluent' },
                { name: 'Docker / Linux Admin', level: 78, status: 'Solid' },
                { name: 'AWS Cloud Services', level: 46, status: 'Target Gap' },
              ].map((sk) => (
                <div key={sk.name} className="space-y-1.5">
                  <div className="flex items-center justify-between text-xs">
                    <span className="text-gray-200 font-medium">{sk.name}</span>
                    <span className="text-gray-400 font-mono text-[11px]">{sk.level}%</span>
                  </div>
                  <div className="w-full bg-white/5 h-2 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-1000 ${
                        sk.level > 80
                          ? 'bg-gradient-to-r from-red-600 to-amber-500'
                          : sk.level > 60
                          ? 'bg-amber-500'
                          : 'bg-rose-500/70'
                      }`}
                      style={{ width: `${sk.level}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>

            <NavLink
              to="/profile"
              className="block text-center text-xs font-mono text-gray-400 hover:text-white pt-2 border-t border-white/5"
            >
              Update Technical Dossier →
            </NavLink>
          </div>

          {/* Upcoming Tasks */}
          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4 text-red-400" />
                <h3 className="font-display font-bold text-base text-white">
                  Upcoming Tactical Tasks
                </h3>
              </div>
              <span className="text-[10px] font-mono text-gray-400">3 PENDING</span>
            </div>

            <div className="space-y-3">
              {[
                {
                  title: 'Complete 10-Question AI Interview',
                  desc: 'Evaluate Java concurrency & transactional locking depth.',
                  date: 'Today',
                  urgent: true,
                  link: '/mentor',
                },
                {
                  title: 'Deploy Spring Boot to AWS ECS',
                  desc: 'Eliminates primary missing requirement for TechNova & Mercari.',
                  date: '3 days left',
                  urgent: false,
                  link: '/report',
                },
                {
                  title: 'Tsukuba Special Screening Review',
                  desc: 'Review vocational diploma credit transfer prerequisites.',
                  date: 'Next week',
                  urgent: false,
                  link: '/aimentor',
                },
              ].map((task, i) => (
                <NavLink
                  key={i}
                  to={task.link}
                  className="block p-3.5 rounded-xl bg-white/[0.02] hover:bg-white/[0.06] border border-white/5 transition-all text-left group"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-gray-200 group-hover:text-red-400 transition-colors">
                      {task.title}
                    </span>
                    <span
                      className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${
                        task.urgent
                          ? 'bg-red-950/60 text-red-400 border border-red-500/30'
                          : 'bg-white/5 text-gray-400'
                      }`}
                    >
                      {task.date}
                    </span>
                  </div>
                  <p className="text-[11px] text-gray-400 mt-1 line-clamp-1">{task.desc}</p>
                </NavLink>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
