import React, { useState, useEffect } from 'react';
import { useNavigate, NavLink } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { roninApi } from '../services/api';
import { FinalReadinessReport, ActionPlanWeek } from '../types/interview';
import {
  Download,
  RotateCcw,
  Sparkles,
  Award,
  CheckCircle2,
  Calendar,
  Building,
  ArrowRight,
  TrendingUp,
  Share2,
  AlertTriangle,
  FileCheck2,
  Briefcase,
  ListTodo,
} from 'lucide-react';
import { ScoreGauge } from '../components/common/ScoreGauge';
import { KanjiBadge } from '../components/common/KanjiBadge';
import confetti from 'canvas-confetti';

export const ReportPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, selectedOpportunityId, addToast } = useApp();

  const [report, setReport] = useState<FinalReadinessReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [roadmap, setRoadmap] = useState<ActionPlanWeek[]>([]);

  useEffect(() => {
    async function loadReport() {
      setLoading(true);
      const rep = await roninApi.generateReport(user?.id || 'thushan', selectedOpportunityId);
      setReport(rep);
      setRoadmap(rep.fourWeekRoadmap);
      setLoading(false);

      // Trigger celebratory samurai confetti on completion!
      try {
        confetti({
          particleCount: 50,
          spread: 70,
          origin: { y: 0.6 },
          colors: ['#E53935', '#D4AF37', '#FFFFFF'],
        });
      } catch {
        // ignore if confetti blocked
      }
    }
    loadReport();
  }, [user?.id, selectedOpportunityId]);

  if (loading || !report) {
    return (
      <div className="py-20 text-center font-mono text-gray-400">
        <Sparkles className="w-8 h-8 text-red-500 animate-spin mx-auto mb-3" />
        <div>GENERATING EXECUTIVE TACTICAL DOSSIER...</div>
      </div>
    );
  }

  const handleToggleTask = (weekNum: number, taskId: string) => {
    setRoadmap((prev) =>
      prev.map((week) => {
        if (week.week === weekNum) {
          return {
            ...week,
            tasks: week.tasks.map((t) => (t.id === taskId ? { ...t, completed: !t.completed } : t)),
          };
        }
        return week;
      })
    );
    addToast('Roadmap milestone updated', 'info');
  };

  const handleDownloadReport = () => {
    addToast('Generating encrypted PDF report package: RONIN_Readiness_Report_Thushan.pdf', 'success');
  };

  return (
    <div className="space-y-8 text-left">
      {/* Top Action Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2 border-b border-white/5">
        <div className="flex items-center gap-3">
          <KanjiBadge kanji="最終指南書" subtext="Final Action Report" variant="crimson" />
          <span className="text-xs font-mono text-gray-400">
            DOCUMENT ID: {report.id} • {report.generatedDate}
          </span>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/aimentor')}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-gray-300 hover:text-white border border-white/10 text-xs font-mono transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Start Another Analysis</span>
          </button>

          <button
            onClick={handleDownloadReport}
            className="flex items-center gap-2 px-5 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-lg shadow-red-600/30"
          >
            <Download className="w-4 h-4" />
            <span>Download PDF Report</span>
          </button>
        </div>
      </div>

      {/* Hero Executive Summary Card */}
      <div className="glass-panel p-6 sm:p-10 rounded-3xl border border-red-500/30 relative overflow-hidden shadow-2xl">
        <div className="absolute top-0 right-0 w-80 h-full bg-gradient-to-l from-red-600/10 via-transparent to-transparent pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-8">
          <div className="space-y-3">
            <div className="text-xs font-mono text-red-400 tracking-wider uppercase font-semibold">
              RONIN AI • CAREER PATH READINESS REPORT
            </div>

            <h1 className="font-display font-black text-3xl sm:text-4xl text-white">
              Tactical Roadmap for <span className="text-red-500">{report.candidateName}</span>
            </h1>

            <div className="flex flex-wrap items-center gap-3 text-xs font-mono text-gray-300">
              <span className="text-white font-semibold flex items-center gap-1.5">
                <Briefcase className="w-4 h-4 text-red-500" />
                Target: {report.targetTitle}
              </span>
              <span>•</span>
              <span className="text-gray-400">{report.organization}</span>
              <span>•</span>
              <span className="text-amber-400">{report.eligibilityRatio}</span>
            </div>

            <p className="text-xs sm:text-sm text-gray-300 max-w-2xl leading-relaxed pt-2">
              {report.summary}
            </p>
          </div>

          {/* Large Readiness Score Gauge */}
          <div className="flex flex-col items-center justify-center shrink-0">
            <ScoreGauge
              score={report.overallReadiness}
              size={150}
              strokeWidth={11}
              label="OVERALL READINESS"
              sublabel="CAREER VIABILITY"
            />
            <span className="mt-2 text-[11px] font-mono text-emerald-400 font-semibold">
              HIGH PLACEMENT PROBABILITY
            </span>
          </div>
        </div>

        {/* 3 Metric Pills */}
        <div className="mt-8 pt-6 border-t border-white/10 grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5">
            <div className="text-xs font-mono text-gray-400">ROLE ALIGNMENT</div>
            <div className="text-2xl font-bold font-display text-white mt-1">
              {report.fitScore}%
            </div>
            <div className="text-[11px] text-emerald-400 font-mono mt-0.5">Top 5% in Cohort</div>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5">
            <div className="text-xs font-mono text-gray-400">INTERVIEW PERFORMANCE</div>
            <div className="text-2xl font-bold font-display text-white mt-1">
              {report.interviewScore} / 100
            </div>
            <div className="text-[11px] text-amber-400 font-mono mt-0.5">STAR method refined</div>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5">
            <div className="text-xs font-mono text-gray-400">ACTIONABLE TIMELINE</div>
            <div className="text-2xl font-bold font-display text-white mt-1">
              4 Weeks
            </div>
            <div className="text-[11px] text-cyan-400 font-mono mt-0.5">Structured execution</div>
          </div>
        </div>
      </div>

      {/* Grid: Strongest Skills vs Missing Gaps */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Strongest Skills */}
        <div className="glass-panel p-6 sm:p-7 rounded-3xl border border-emerald-500/30 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-emerald-400 text-xs font-mono font-bold uppercase">
              <CheckCircle2 className="w-4 h-4" />
              <span>Verified Key Strengths</span>
            </div>
            <KanjiBadge kanji="強み" variant="gold" />
          </div>

          <div className="space-y-2.5">
            {report.strongestSkills.map((sk, idx) => (
              <div
                key={idx}
                className="flex items-center gap-3 p-3 rounded-xl bg-emerald-950/20 border border-emerald-500/20 text-xs text-gray-200"
              >
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                <span className="font-semibold text-white">{sk}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Missing Skills */}
        <div className="glass-panel p-6 sm:p-7 rounded-3xl border border-amber-500/30 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-amber-400 text-xs font-mono font-bold uppercase">
              <AlertTriangle className="w-4 h-4" />
              <span>Identified Skill Gaps to Close</span>
            </div>
            <KanjiBadge kanji="弱点克服" variant="crimson" />
          </div>

          <div className="space-y-2.5">
            {report.missingSkills.map((sk, idx) => (
              <div
                key={idx}
                className="flex items-center gap-3 p-3 rounded-xl bg-amber-950/20 border border-amber-500/20 text-xs text-gray-200"
              >
                <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
                <span className="font-semibold text-white">{sk}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 30-Day Action Plan Roadmap Visualization */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/10 space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <Calendar className="w-5 h-5 text-red-500" />
              <h2 className="font-display font-bold text-xl text-white">
                30-Day Tactical Execution Roadmap
              </h2>
            </div>
            <p className="text-xs text-gray-400 mt-1">
              Follow this phased weekly sprint to eliminate gaps, optimize presentation, and apply with maximum competitive leverage.
            </p>
          </div>

          <KanjiBadge kanji="三十日計画" variant="crimson" />
        </div>

        {/* 4 Weekly Phased Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 pt-2">
          {roadmap.map((week) => (
            <div
              key={week.week}
              className="p-5 sm:p-6 rounded-2xl bg-white/[0.02] border border-white/10 hover:border-red-500/40 transition-all flex flex-col justify-between space-y-4 group"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="font-mono text-xs font-bold text-red-500 bg-red-950/60 px-2.5 py-0.5 rounded border border-red-500/30">
                    WEEK {week.week}
                  </span>
                  <span className="text-[11px] font-mono text-gray-400 uppercase">
                    FOCUS: {week.focusArea}
                  </span>
                </div>

                <h3 className="font-display font-bold text-base text-white group-hover:text-red-400 transition-colors">
                  {week.title}
                </h3>
                <p className="text-xs text-gray-400 mt-1 leading-relaxed">
                  {week.description}
                </p>
              </div>

              {/* Task list with interactive checkboxes */}
              <div className="space-y-2 pt-2 border-t border-white/5">
                <span className="text-[10px] font-mono text-gray-500 uppercase flex items-center gap-1">
                  <ListTodo className="w-3 h-3" /> ACTION ITEMS:
                </span>
                {week.tasks.map((task) => (
                  <div
                    key={task.id}
                    onClick={() => handleToggleTask(week.week, task.id)}
                    className="flex items-start gap-2.5 p-2 rounded-lg bg-white/[0.02] hover:bg-white/[0.06] cursor-pointer text-xs transition-colors"
                  >
                    <input
                      type="checkbox"
                      checked={task.completed}
                      onChange={() => {}} // handled by parent div
                      className="mt-0.5 accent-red-600 rounded cursor-pointer"
                    />
                    <span
                      className={`leading-relaxed ${
                        task.completed ? 'line-through text-gray-500' : 'text-gray-200'
                      }`}
                    >
                      {task.text}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recommended Parallel Opportunities */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/10 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-display font-bold text-lg text-white">
            Recommended Parallel Target Pipeline
          </h3>
          <NavLink
            to="/aimentor"
            className="text-xs font-mono text-red-400 hover:text-red-300 flex items-center gap-1"
          >
            <span>View All Matches</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </NavLink>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {[
            { title: 'FinTech Backend Developer', org: 'Mercari Japan', match: 94, id: 'job-4' },
            { title: 'System Engineer (Junior)', org: 'Rakuten Cloud', match: 86, id: 'job-2' },
            { title: 'Master in Informatics', org: 'University of Tsukuba', match: 93, id: 'uni-2' },
          ].map((item) => (
            <div
              key={item.id}
              onClick={() => navigate(`/opportunity/${item.id}`)}
              className="p-4 rounded-2xl bg-white/[0.02] hover:bg-white/[0.06] border border-white/5 hover:border-red-500/40 cursor-pointer transition-all text-left"
            >
              <div className="flex items-center justify-between mb-1">
                <span className="text-[10px] font-mono text-gray-400 uppercase">{item.org}</span>
                <span className="text-xs font-mono font-bold text-emerald-400">{item.match}%</span>
              </div>
              <div className="text-sm font-bold text-white truncate">{item.title}</div>
              <div className="text-[11px] font-mono text-red-400 mt-2">View Analysis →</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
