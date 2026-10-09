import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { roninApi } from '../services/api';
import { InterviewAnalysisResult } from '../types/interview';
import {
  ResponsiveContainer,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
} from 'recharts';
import {
  Sparkles,
  Award,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  TrendingUp,
  Brain,
  MessageSquare,
  Bot,
  Building,
} from 'lucide-react';
import { KanjiBadge } from '../components/common/KanjiBadge';
import { ScoreGauge } from '../components/common/ScoreGauge';

export const InterviewAnalysisPage: React.FC = () => {
  const navigate = useNavigate();
  const { user, selectedOpportunityId, addToast } = useApp();

  const [analysis, setAnalysis] = useState<InterviewAnalysisResult | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [isGeneratingReport, setIsGeneratingReport] = useState<boolean>(false);

  useEffect(() => {
    async function loadAnalysis() {
      setLoading(true);
      const res = await roninApi.getInterviewAnalysis();
      setAnalysis(res);
      setLoading(false);
    }
    loadAnalysis();
  }, []);

  if (loading || !analysis) {
    return (
      <div className="py-20 text-center font-mono text-gray-400">
        <Sparkles className="w-8 h-8 text-red-500 animate-spin mx-auto mb-3" />
        <div>COMPUTING PSYCHOMETRIC & TECHNICAL PERFORMANCE VECTORS...</div>
      </div>
    );
  }

  const handleGenerateFinalReport = async () => {
    setIsGeneratingReport(true);
    addToast('Synthesizing comprehensive 30-day action roadmap...', 'info');
    await roninApi.generateReport(user?.id || 'thushan', selectedOpportunityId);
    setTimeout(() => {
      setIsGeneratingReport(false);
      navigate('/report');
    }, 800);
  };

  const barData = [
    { name: 'Technical', score: analysis.technicalKnowledge, benchmark: 75 },
    { name: 'Problem Solving', score: analysis.problemSolving, benchmark: 72 },
    { name: 'Job Fit', score: analysis.jobFit, benchmark: 80 },
    { name: 'Communication', score: analysis.communication, benchmark: 75 },
    { name: 'Experience Evidence', score: analysis.experienceEvidence, benchmark: 70 },
  ];

  return (
    <div className="space-y-8 text-left">
      {/* Header Banner */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-red-500/30 relative overflow-hidden shadow-2xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <KanjiBadge kanji="面接診断" subtext="Evaluation Audit" variant="crimson" />
              <span className="text-xs font-mono text-gray-400">
                SESSION ID: {analysis.interviewId} • DURATION: {analysis.durationMinutes}m
              </span>
            </div>
            <h1 className="font-display font-black text-2xl sm:text-3xl text-white">
              Interview Performance Diagnostic
            </h1>
            <p className="text-xs sm:text-sm text-gray-300 font-sans max-w-xl">
              Target Position:{' '}
              <span className="text-white font-semibold">{analysis.targetRole}</span> at{' '}
              <span className="text-red-400 font-semibold">{analysis.organization}</span>
            </p>
          </div>

          {/* Overall Score */}
          <div className="flex items-center gap-6">
            <div className="text-right hidden sm:block">
              <div className="text-xs font-mono text-gray-400 uppercase">SYNTHESIZED RATING</div>
              <div className="text-sm font-semibold text-emerald-400">Competitive Tier 1</div>
            </div>
            <ScoreGauge
              score={analysis.overallScore}
              size={130}
              strokeWidth={10}
              label="OVERALL SCORE"
              sublabel="OUT OF 100"
            />
          </div>
        </div>

        {/* Generate Report Button Header CTA */}
        <div className="mt-6 pt-5 border-t border-white/10 flex flex-wrap items-center justify-between gap-4">
          <div className="text-xs font-mono text-gray-400 flex items-center gap-2">
            <Bot className="w-4 h-4 text-amber-400" />
            <span>AI Interview evaluation complete across 10 technical competency modules</span>
          </div>

          <button
            onClick={handleGenerateFinalReport}
            disabled={isGeneratingReport}
            className="flex items-center gap-2.5 px-7 py-3 rounded-xl bg-gradient-to-r from-red-600 via-rose-600 to-amber-600 hover:from-red-500 hover:to-amber-500 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-xl shadow-red-600/30"
          >
            {isGeneratingReport ? (
              <>
                <Sparkles className="w-4 h-4 animate-spin" />
                <span>Compiling Strategy...</span>
              </>
            ) : (
              <>
                <span>Generate Final Report</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </div>
      </div>

      {/* Recharts Analytics Grid: Radar Chart + Category Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left (6 cols): Recharts Radar Chart */}
        <div className="lg:col-span-6 glass-panel p-6 sm:p-7 rounded-3xl border border-white/10 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Brain className="w-4 h-4 text-red-500" />
              <h3 className="font-display font-bold text-base text-white">
                Competency Radar Analysis
              </h3>
            </div>
            <span className="text-[10px] font-mono text-gray-400">CANDIDATE VS BENCHMARK</span>
          </div>

          {/* Radar Chart Canvas */}
          <div className="w-full h-72 sm:h-80">
            <ResponsiveContainer width="100%" height="100%">
              <RadarChart data={analysis.radarData} margin={{ top: 10, right: 20, bottom: 10, left: 20 }}>
                <PolarGrid stroke="rgba(255, 255, 255, 0.1)" />
                <PolarAngleAxis
                  dataKey="metric"
                  tick={{ fill: '#9CA3AF', fontSize: 11, fontFamily: 'monospace' }}
                />
                <PolarRadiusAxis
                  angle={30}
                  domain={[0, 100]}
                  tick={{ fill: '#6B7280', fontSize: 9 }}
                />
                <Radar
                  name="Benchmark"
                  dataKey="benchmark"
                  stroke="#6B7280"
                  fill="#6B7280"
                  fillOpacity={0.15}
                />
                <Radar
                  name="Candidate (Thushan)"
                  dataKey="score"
                  stroke="#E53935"
                  fill="#E53935"
                  fillOpacity={0.45}
                />
              </RadarChart>
            </ResponsiveContainer>
          </div>

          <div className="flex items-center justify-center gap-6 text-xs font-mono">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded bg-red-600" />
              <span className="text-gray-200">Candidate Score</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded bg-gray-600" />
              <span className="text-gray-400">Industry Benchmark</span>
            </div>
          </div>
        </div>

        {/* Right (6 cols): 5 Metrics Progress Bars */}
        <div className="lg:col-span-6 glass-panel p-6 sm:p-7 rounded-3xl border border-white/10 space-y-5">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Award className="w-4 h-4 text-amber-400" />
              <h3 className="font-display font-bold text-base text-white">
                Core Metrics Breakdown
              </h3>
            </div>
            <KanjiBadge kanji="五輪評価" variant="gold" />
          </div>

          <div className="space-y-4">
            {[
              { label: 'Technical Knowledge', val: analysis.technicalKnowledge, desc: 'Spring Boot architecture & concurrency' },
              { label: 'Problem Solving', val: analysis.problemSolving, desc: 'Pessimistic locking and race condition triage' },
              { label: 'Job Fit', val: analysis.jobFit, desc: 'High synergy with TechNova tech requirements' },
              { label: 'Communication', val: answerScoreHelper(analysis.communication), desc: 'Structure and conciseness in answers' },
              { label: 'Experience Evidence', val: analysis.experienceEvidence, desc: 'Quantifiable metrics from production/capstones' },
            ].map((m) => (
              <div key={m.label} className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <div>
                    <span className="font-bold text-white">{m.label}</span>
                    <span className="text-gray-400 text-[11px] ml-2 hidden sm:inline font-mono">
                      ({m.desc})
                    </span>
                  </div>
                  <span className="font-mono font-bold text-white text-sm">{m.val}%</span>
                </div>
                <div className="w-full bg-white/5 h-2 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-1000 ${
                      m.val >= 85
                        ? 'bg-emerald-500'
                        : m.val >= 75
                        ? 'bg-amber-500'
                        : 'bg-red-500'
                    }`}
                    style={{ width: `${m.val}%` }}
                  />
                </div>
              </div>
            ))}
          </div>

          {/* AI Overall Assessment Quote */}
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/10 text-xs text-gray-300 leading-relaxed italic">
            "{analysis.aiOverallFeedback}"
          </div>
        </div>
      </div>

      {/* Strong Answers & Needs Improvement */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Strong Answers */}
        <div className="glass-panel p-6 rounded-3xl border border-emerald-500/30 space-y-3">
          <div className="flex items-center gap-2 text-emerald-400 text-xs font-mono font-bold uppercase">
            <CheckCircle2 className="w-4 h-4" />
            <span>Demonstrated Strengths</span>
          </div>
          <ul className="space-y-2.5 text-xs text-gray-300">
            {analysis.strongAnswers.map((ans, idx) => (
              <li key={idx} className="flex items-start gap-2.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0 mt-0.5" />
                <span>{ans}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Needs Improvement */}
        <div className="glass-panel p-6 rounded-3xl border border-amber-500/30 space-y-3">
          <div className="flex items-center gap-2 text-amber-400 text-xs font-mono font-bold uppercase">
            <AlertTriangle className="w-4 h-4" />
            <span>Target Areas for Improvement</span>
          </div>
          <ul className="space-y-2.5 text-xs text-gray-300">
            {analysis.needsImprovement.map((item, idx) => (
              <li key={idx} className="flex items-start gap-2.5">
                <AlertTriangle className="w-3.5 h-3.5 text-amber-400 shrink-0 mt-0.5" />
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* STAR Method Coaching Suggestion (Required by Prompt) */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-red-500/30 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-red-500" />
            <h3 className="font-display font-bold text-lg text-white">
              Recommended STAR Answer Template
            </h3>
          </div>
          <KanjiBadge kanji="模範回答" variant="crimson" />
        </div>

        <p className="text-xs text-gray-300 font-sans">
          To transition from a 78 to an 90+ score in Japanese technical rounds, present your capstone challenges using this structured STAR formula:
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 pt-2">
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 space-y-1.5">
            <span className="text-[10px] font-mono text-red-400 font-bold uppercase block">
              S — SITUATION (状況)
            </span>
            <p className="text-xs text-gray-300 leading-relaxed">
              {analysis.starSuggestion.situation}
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 space-y-1.5">
            <span className="text-[10px] font-mono text-amber-400 font-bold uppercase block">
              T — TASK (課題)
            </span>
            <p className="text-xs text-gray-300 leading-relaxed">
              {analysis.starSuggestion.task}
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 space-y-1.5">
            <span className="text-[10px] font-mono text-cyan-400 font-bold uppercase block">
              A — ACTION (行動)
            </span>
            <p className="text-xs text-gray-300 leading-relaxed">
              {analysis.starSuggestion.action}
            </p>
          </div>

          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 space-y-1.5">
            <span className="text-[10px] font-mono text-emerald-400 font-bold uppercase block">
              R — RESULT (成果)
            </span>
            <p className="text-xs text-gray-300 leading-relaxed">
              {analysis.starSuggestion.result}
            </p>
          </div>
        </div>
      </div>

      {/* Bottom CTA to Final Report */}
      <div className="flex justify-end pt-2">
        <button
          onClick={handleGenerateFinalReport}
          className="flex items-center gap-3 px-8 py-4 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white font-bold text-sm tracking-wide shadow-xl shadow-red-600/30 transition-all transform hover:-translate-y-0.5"
        >
          <span>Generate Final 30-Day Report</span>
          <ArrowRight className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};

function answerScoreHelper(score: number) {
  return score;
}
