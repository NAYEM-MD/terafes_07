import React from 'react';
import { NavLink } from 'react-router-dom';
import { useStartJourney } from '../hooks/useStartJourney';
import {
  ArrowRight,
  Sparkles,
  ShieldCheck,
  Cpu,
  GraduationCap,
  Briefcase,
  Compass,
  FileCheck2,
  Bot,
  Flame,
  CheckCircle2,
} from 'lucide-react';
import { MonEmblem } from '../components/common/MonEmblem';
import { KanjiBadge } from '../components/common/KanjiBadge';
import { AIMentorAvatar } from '../components/mentor/AIMentorAvatar';

export const LandingPage: React.FC = () => {
  const { startJourney, pending, authReady } = useStartJourney();
  const journeyDisabled = !authReady || pending;

  return (
    <div className="relative min-h-screen bg-[#090A0F] text-gray-100 overflow-x-hidden selection:bg-red-600 selection:text-white">
      {/* Background Cyber-Grid & Ambient Crimson Glow */}
      <div className="fixed inset-0 cyber-grid opacity-30 pointer-events-none" />
      <div className="fixed top-0 left-1/2 -translate-x-1/2 w-[900px] h-[500px] bg-red-600/10 blur-[140px] pointer-events-none rounded-full" />
      <div className="fixed bottom-0 right-0 w-[600px] h-[500px] bg-amber-600/5 blur-[160px] pointer-events-none rounded-full" />

      {/* Navigation Header */}
      <header className="relative z-20 max-w-7xl mx-auto px-6 py-6 flex items-center justify-between">
        <NavLink to="/welcome" className="flex items-center gap-3.5 group">
          <MonEmblem size={40} />
          <div>
            <div className="flex items-center gap-2">
              <span className="font-display font-black text-2xl tracking-wider text-white">
                RONIN
              </span>
              <span className="px-1.5 py-0.5 rounded text-[11px] font-mono font-bold bg-red-600/20 text-red-400 border border-red-500/30">
                AI
              </span>
            </div>
            <span className="text-[10px] text-gray-400 tracking-widest font-mono block">
              FORGE YOUR OWN PATH
            </span>
          </div>
        </NavLink>

        <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-300">
          <a href="#process" className="hover:text-white transition-colors">
            Protocol Process
          </a>
          <a href="#modes" className="hover:text-white transition-colors">
            Dual Pathways
          </a>
          <a href="#features" className="hover:text-white transition-colors">
            AI Technology
          </a>
        </nav>

        <div className="flex items-center gap-4">
          <NavLink
            to="/dashboard"
            className="hidden sm:inline-flex px-4 py-2 rounded-xl text-xs font-semibold text-gray-300 hover:text-white hover:bg-white/5 border border-white/10 transition-all font-mono"
          >
            Terminal Access
          </NavLink>
          <button
            type="button"
            onClick={() => startJourney()}
            disabled={journeyDisabled}
            aria-busy={pending}
            className="relative z-20 flex shrink-0 items-center gap-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 disabled:opacity-60 text-white text-xs font-bold uppercase tracking-wider shadow-lg shadow-red-600/30 hover:shadow-red-600/50 transition-all focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
          >
            <span>{pending ? 'Checking...' : 'Start Journey'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative z-10 max-w-7xl mx-auto px-6 pt-12 pb-24 lg:pt-16 lg:pb-32 grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
        {/* Left Column: Mission & CTAs */}
        <div className="lg:col-span-7 space-y-8 text-left">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-red-950/40 border border-red-500/30 backdrop-blur-md">
            <Flame className="w-4 h-4 text-red-500 animate-pulse" />
            <span className="text-xs font-mono font-medium text-red-300 uppercase tracking-wider">
              AI Career & University Intelligence Dojo
            </span>
          </div>

          <div className="space-y-4">
            <h1 className="font-display font-black text-5xl sm:text-6xl lg:text-7xl tracking-tight text-white leading-[1.05]">
              RONIN <span className="text-red-500">AI</span>
            </h1>
            <p className="font-display text-2xl sm:text-3xl text-amber-300/90 font-medium tracking-wide">
              Forge Your Own Path.
            </p>
            <p className="text-base sm:text-lg text-gray-300 max-w-2xl leading-relaxed font-sans pt-2">
              An AI mentor that analyzes your background, discovers high-affinity opportunities,
              prepares you through personalized face-to-face style interviews, and guides you
              forward with an actionable 30-day strategy.
            </p>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-wrap items-center gap-4 pt-2">
            <button
              type="button"
              onClick={() => startJourney()}
              disabled={journeyDisabled}
              aria-busy={pending}
              className="relative z-20 flex items-center gap-3 px-8 py-4 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 disabled:opacity-60 text-white font-bold text-sm tracking-wide shadow-xl shadow-red-600/30 hover:shadow-red-600/50 transition-all transform hover:-translate-y-0.5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
            >
              <span>{pending ? 'Checking account...' : 'Start Your Journey'}</span>
              <ArrowRight className="w-5 h-5" />
            </button>

            <NavLink
              to="/dashboard"
              className="flex items-center gap-3 px-7 py-4 rounded-xl bg-[#141724]/90 hover:bg-[#1B1F30] text-gray-200 hover:text-white font-semibold text-sm border border-white/10 hover:border-white/20 transition-all backdrop-blur-md"
            >
              <Cpu className="w-4 h-4 text-amber-400" />
              <span>View Interactive Demo</span>
            </NavLink>
          </div>

          {/* Key Value Highlights */}
          <div className="pt-8 border-t border-white/10 grid grid-cols-3 gap-6">
            <div>
              <div className="text-2xl font-bold font-display text-white">92%</div>
              <div className="text-xs text-gray-400 font-mono mt-0.5">Eligibility Accuracy</div>
            </div>
            <div>
              <div className="text-2xl font-bold font-display text-amber-400">1-on-1</div>
              <div className="text-xs text-gray-400 font-mono mt-0.5">Avatar Simulation</div>
            </div>
            <div>
              <div className="text-2xl font-bold font-display text-red-500">STAR</div>
              <div className="text-xs text-gray-400 font-mono mt-0.5">Method Diagnostics</div>
            </div>
          </div>
        </div>

        {/* Right Column: Large Futuristic AI Mentor Portrait */}
        <div className="lg:col-span-5 flex flex-col items-center justify-center relative">
          <div className="absolute -top-6 -right-6 hidden sm:block">
            <KanjiBadge kanji="指南役" subtext="Master Mentor" variant="crimson" />
          </div>

          {/* Interactive Mentor Avatar Card (Hero size with state controls) */}
          <AIMentorAvatar state="speaking" size="hero" showControls={true} />

          <p className="mt-4 text-[11px] font-mono text-gray-400 text-center max-w-sm">
            Powered by adaptive Japanese-English NLP & neural avatar feedback framework. Ready for LivePortrait / MuseTalk integration.
          </p>
        </div>
      </section>

      {/* Section 2: Process Steps Flow */}
      <section id="process" className="relative z-10 py-20 border-y border-white/5 bg-[#0C0E16]/60">
        <div className="max-w-7xl mx-auto px-6">
          <div className="text-center max-w-2xl mx-auto space-y-3 mb-16">
            <KanjiBadge kanji="修練の道" subtext="Fivefold Progression" variant="gold" />
            <h2 className="font-display font-bold text-3xl sm:text-4xl text-white">
              The Path of Mastery
            </h2>
            <p className="text-gray-400 text-sm">
              From personal profile synthesis to personalized interview mastery and actionable execution.
            </p>
          </div>

          {/* 5-Step Process Pipeline */}
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4 relative">
            {[
              {
                step: '01',
                title: 'Profile Dossier',
                kanji: '武録',
                desc: 'Map skills, education, languages (JLPT/IELTS), and certifications.',
                icon: ShieldCheck,
              },
              {
                step: '02',
                title: 'AiMentor',
                kanji: '探索',
                desc: 'RAG algorithmic scan across curated Japanese jobs and universities.',
                icon: Compass,
              },
              {
                step: '03',
                title: 'AI Interview',
                kanji: '道場',
                desc: 'Face-to-face simulated grilling with your adaptive cyber mentor avatar.',
                icon: Bot,
              },
              {
                step: '04',
                title: 'Gap Analysis',
                kanji: '診断',
                desc: 'Radar performance scoring, evidence validation, and STAR critique.',
                icon: Cpu,
              },
              {
                step: '05',
                title: 'Action Plan',
                kanji: '指南',
                desc: 'A prioritized 30-day execution roadmap to secure your top offer.',
                icon: FileCheck2,
              },
            ].map((item, idx) => {
              const Icon = item.icon;
              return (
                <div
                  key={item.step}
                  className="glass-panel glass-panel-hover p-5 rounded-2xl flex flex-col justify-between text-left relative overflow-hidden group"
                >
                  <div className="flex items-center justify-between mb-4">
                    <span className="font-mono text-xs font-bold text-red-500 bg-red-950/40 px-2 py-0.5 rounded border border-red-500/20">
                      STEP {item.step}
                    </span>
                    <span className="font-japanese text-sm text-gray-500 group-hover:text-amber-400 transition-colors">
                      {item.kanji}
                    </span>
                  </div>

                  <div className="my-2">
                    <div className="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center text-red-400 mb-3 group-hover:bg-red-600 group-hover:text-white transition-all">
                      <Icon className="w-5 h-5" />
                    </div>
                    <h3 className="font-bold text-white text-base">{item.title}</h3>
                    <p className="text-xs text-gray-400 mt-2 leading-relaxed">{item.desc}</p>
                  </div>

                  <div className="mt-4 pt-3 border-t border-white/5 flex items-center gap-1.5 text-[11px] font-mono text-gray-500 group-hover:text-gray-300">
                    <span>STATUS: READY</span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Section 3: Dual Mode Selection (Career vs University) */}
      <section id="modes" className="relative z-10 py-24 max-w-7xl mx-auto px-6">
        <div className="text-center max-w-2xl mx-auto space-y-3 mb-16">
          <KanjiBadge kanji="二大流派" subtext="Dual Strategy Modes" variant="crimson" />
          <h2 className="font-display font-bold text-3xl sm:text-4xl text-white">
            Choose Your Destination
          </h2>
          <p className="text-gray-400 text-sm">
            Whether targeting Japan’s premier tech enterprises or world-class postgraduate research institutions.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {/* Career Mode Card */}
          <div className="glass-panel p-8 rounded-3xl text-left border border-red-500/20 hover:border-red-500/40 transition-all relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-8 opacity-10 group-hover:opacity-20 transition-opacity">
              <Briefcase className="w-36 h-36 text-red-500" />
            </div>

            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 rounded-2xl bg-red-600/20 border border-red-500/30 text-red-400">
                <Briefcase className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs font-mono uppercase text-red-400">MODE 01</span>
                <h3 className="font-display font-bold text-2xl text-white">Career Path Mode</h3>
              </div>
            </div>

            <p className="text-sm text-gray-300 leading-relaxed mb-6">
              Designed for job seekers and new graduates targeting software engineering, system engineering,
              and cloud architecture roles in Tokyo.
            </p>

            <ul className="space-y-2.5 text-xs text-gray-300 mb-8">
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Matches against 8+ curated Japanese tech enterprise profiles</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Simulates technical screens in Java, Spring Boot, and databases</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Audits JLPT N2/N1 bilingual communication preparedness</span>
              </li>
            </ul>

            <button
              type="button"
              onClick={() => startJourney('job')}
              disabled={journeyDisabled}
              className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-60 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-lg shadow-red-600/30 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
            >
              <span>Explore Career Mode</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>

          {/* University Mode Card */}
          <div className="glass-panel p-8 rounded-3xl text-left border border-amber-500/20 hover:border-amber-500/40 transition-all relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-8 opacity-10 group-hover:opacity-20 transition-opacity">
              <GraduationCap className="w-36 h-36 text-amber-500" />
            </div>

            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 rounded-2xl bg-amber-600/20 border border-amber-500/30 text-amber-400">
                <GraduationCap className="w-6 h-6" />
              </div>
              <div>
                <span className="text-xs font-mono uppercase text-amber-400">MODE 02</span>
                <h3 className="font-display font-bold text-2xl text-white">University Path Mode</h3>
              </div>
            </div>

            <p className="text-sm text-gray-300 leading-relaxed mb-6">
              Tailored for vocational college and bachelor graduates seeking Master or Bachelor transfer
              degrees across Japan’s top research institutions.
            </p>

            <ul className="space-y-2.5 text-xs text-gray-300 mb-8">
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-amber-400 shrink-0" />
                <span>Eligibility checks for Todai, Tokyo Tech, Tsukuba, Waseda & Kyoto</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-amber-400 shrink-0" />
                <span>Special screening guidelines for IT vocational degree candidates</span>
              </li>
              <li className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-amber-400 shrink-0" />
                <span>Research proposal and academic interview simulator</span>
              </li>
            </ul>

            <button
              type="button"
              onClick={() => startJourney('university')}
              disabled={journeyDisabled}
              className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-amber-600 hover:bg-amber-500 disabled:opacity-60 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-lg shadow-amber-600/30 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-400"
            >
              <span>Explore University Mode</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="relative z-10 py-12 border-t border-white/5 bg-[#07080D]">
        <div className="max-w-7xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-3">
            <MonEmblem size={28} />
            <span className="font-display font-bold text-white text-sm">
              RONIN AI <span className="text-gray-500 font-sans font-normal text-xs">| Forge Your Own Path</span>
            </span>
          </div>

          <div className="text-xs text-gray-500 font-mono">
            ENGINEERED FOR ASPIRING BUILDERS & SCHOLARS IN JAPAN • 2026-2027 INTAKE
          </div>

          <NavLink
            to="/dashboard"
            className="text-xs font-semibold text-red-400 hover:text-red-300 transition-colors font-mono"
          >
            Enter Dashboard →
          </NavLink>
        </div>
      </footer>
    </div>
  );
};
