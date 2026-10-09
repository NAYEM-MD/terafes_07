import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, NavLink } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { roninApi } from '../services/api';
import { Opportunity } from '../types/opportunity';
import {
  ArrowLeft,
  Bot,
  Bookmark,
  CheckCircle2,
  AlertTriangle,
  Building,
  MapPin,
  Sparkles,
  FileText,
  ShieldCheck,
  Cpu,
  Layers,
  Share2,
} from 'lucide-react';
import { ScoreGauge } from '../components/common/ScoreGauge';
import { KanjiBadge } from '../components/common/KanjiBadge';

export const OpportunityDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const {
    selectedOpportunityId,
    setSelectedOpportunityId,
    toggleSaveOpportunity,
    savedOpportunityIds,
    addToast,
  } = useApp();

  const activeId = id || selectedOpportunityId || 'job-1';
  const [opportunity, setOpportunity] = useState<Opportunity | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function loadOpportunity() {
      setLoading(true);
      const data = await roninApi.getOpportunityById(activeId);
      setOpportunity(data);
      if (data) {
        setSelectedOpportunityId(data.id);
      }
      setLoading(false);
    }
    loadOpportunity();
  }, [activeId, setSelectedOpportunityId]);

  if (loading) {
    return (
      <div className="py-20 text-center font-mono text-gray-400">
        <Sparkles className="w-8 h-8 text-red-500 animate-spin mx-auto mb-3" />
        <div>SYNTHESIZING DEEP VECTOR ANALYSIS...</div>
      </div>
    );
  }

  if (!opportunity) {
    return (
      <div className="py-20 text-center space-y-4">
        <div className="text-xl font-bold text-white">Target Opportunity Not Found</div>
        <NavLink
          to="/discover"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 text-white text-xs font-bold"
        >
          Return to Radar
        </NavLink>
      </div>
    );
  }

  const isSaved = savedOpportunityIds.includes(opportunity.id);
  const satisfiedCount = opportunity.criteriaList.filter((c) => c.isSatisfied).length;
  const totalCount = opportunity.criteriaList.length;

  const handleStartInterview = () => {
    setSelectedOpportunityId(opportunity.id);
    navigate('/mentor');
  };

  return (
    <div className="space-y-8 text-left">
      {/* Top Back Navigation Bar */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/discover')}
          className="flex items-center gap-2 text-xs font-mono text-gray-400 hover:text-white transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>RETURN TO DISCOVERY RADAR</span>
        </button>

        <div className="flex items-center gap-3">
          <button
            onClick={() => {
              navigator.clipboard?.writeText(window.location.href);
              addToast('Opportunity link copied to clipboard', 'info');
            }}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white border border-white/5 transition-colors"
          >
            <Share2 className="w-4 h-4" />
          </button>
          <button
            onClick={() => toggleSaveOpportunity(opportunity.id)}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-gray-200 text-xs font-semibold border border-white/10 transition-colors"
          >
            <Bookmark className={`w-4 h-4 ${isSaved ? 'fill-amber-400 text-amber-400' : ''}`} />
            <span>{isSaved ? 'Saved to Shortlist' : 'Save Opportunity'}</span>
          </button>
        </div>
      </div>

      {/* Main Analysis Header Hero */}
      <div className="glass-panel p-6 sm:p-10 rounded-3xl border border-red-500/30 relative overflow-hidden shadow-2xl">
        <div className="absolute top-0 right-0 w-96 h-full bg-gradient-to-l from-red-600/10 via-transparent to-transparent pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-8">
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <KanjiBadge kanji="適合度解析" subtext="AI Fit Analysis" variant="crimson" />
              <span className="text-xs font-mono text-gray-400">
                POSTED: {opportunity.postedDate || 'Active'}
              </span>
            </div>

            <h1 className="font-display font-black text-3xl sm:text-4xl text-white">
              {opportunity.title}
            </h1>

            <div className="flex flex-wrap items-center gap-4 text-xs font-mono text-gray-300">
              <span className="flex items-center gap-1.5 text-white font-semibold">
                <Building className="w-4 h-4 text-red-500" />
                {opportunity.organization}
              </span>
              <span>•</span>
              <span className="flex items-center gap-1.5 text-gray-300">
                <MapPin className="w-4 h-4 text-gray-400" />
                {opportunity.location}
              </span>
              <span>•</span>
              <span className="text-amber-400">{opportunity.salaryOrTuition}</span>
            </div>

            <p className="text-xs sm:text-sm text-gray-300 max-w-2xl leading-relaxed pt-2">
              {opportunity.description}
            </p>
          </div>

          {/* Large Circular Match Gauge */}
          <div className="flex flex-col items-center justify-center shrink-0">
            <ScoreGauge
              score={opportunity.matchScore}
              size={150}
              strokeWidth={11}
              label="OVERALL MATCH"
              sublabel="ALGORITHMIC AFFINITY"
            />
            <span className="mt-2 text-[11px] font-mono text-gray-400">
              TIER: <span className="text-white font-semibold">{opportunity.matchTier}</span>
            </span>
          </div>
        </div>

        {/* Primary Action Banner */}
        <div className="mt-8 pt-6 border-t border-white/10 flex flex-wrap items-center justify-between gap-4">
          <div className="text-xs font-mono text-gray-400 flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>
              Based on the published requirements, your profile appears to satisfy{' '}
              <strong className="text-white font-semibold">
                {satisfiedCount} of {totalCount}
              </strong>{' '}
              listed criteria.
            </span>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleStartInterview}
              className="flex items-center gap-2.5 px-7 py-3.5 rounded-xl bg-gradient-to-r from-red-600 via-rose-600 to-amber-600 hover:from-red-500 hover:to-amber-500 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-xl shadow-red-600/30 transform hover:-translate-y-0.5"
            >
              <Bot className="w-4 h-4" />
              <span>Start Mentor Interview</span>
            </button>
          </div>
        </div>
      </div>

      {/* Grid: Eligibility Checklist & Evidence + AI Feedback */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left Column (7 cols): Eligibility & Evidence Breakdown */}
        <div className="lg:col-span-7 space-y-6">
          {/* Eligibility Requirements Table */}
          <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/10 space-y-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Layers className="w-4 h-4 text-red-500" />
                <h2 className="font-display font-bold text-lg text-white">
                  Eligibility Criteria & Evidence Breakdown
                </h2>
              </div>
              <span className="text-xs font-mono text-gray-400">
                {satisfiedCount}/{totalCount} MATCHED
              </span>
            </div>

            <div className="space-y-3">
              {opportunity.criteriaList.map((crit) => (
                <div
                  key={crit.id}
                  className={`p-4 rounded-2xl border transition-all ${
                    crit.isSatisfied
                      ? 'bg-emerald-950/20 border-emerald-500/30'
                      : 'bg-amber-950/20 border-amber-500/30'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-2.5">
                      {crit.isSatisfied ? (
                        <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                      ) : (
                        <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0" />
                      )}
                      <div>
                        <div className="text-sm font-bold text-white">{crit.name}</div>
                        <div className="text-xs text-gray-300 mt-0.5">{crit.notes}</div>
                      </div>
                    </div>
                    <span
                      className={`text-[10px] font-mono px-2 py-0.5 rounded font-semibold ${
                        crit.isSatisfied
                          ? 'bg-emerald-500/20 text-emerald-300'
                          : 'bg-amber-500/20 text-amber-300'
                      }`}
                    >
                      {crit.isSatisfied ? 'SATISFIED' : 'GROWTH GAP'}
                    </span>
                  </div>

                  {/* Profile Evidence Citation */}
                  {crit.userEvidence && (
                    <div className="mt-3 pt-2.5 border-t border-white/5 flex items-center gap-2 text-xs font-mono text-gray-400">
                      <FileText className="w-3.5 h-3.5 text-gray-500" />
                      <span>EVIDENCE FROM PROFILE:</span>
                      <span className="text-gray-200 font-sans font-medium">
                        "{crit.userEvidence}"
                      </span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column (5 cols): AI Explanation, Strengths, and Gaps */}
        <div className="lg:col-span-5 space-y-6">
          {/* AI Explanation Callout */}
          <div className="glass-panel p-6 sm:p-7 rounded-3xl border border-red-500/30 space-y-3 relative overflow-hidden">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-amber-400" />
              <h3 className="font-display font-bold text-sm text-white uppercase tracking-wider">
                RONIN AI Tactical Assessment
              </h3>
            </div>
            <p className="text-xs sm:text-sm text-gray-200 leading-relaxed font-sans">
              "{opportunity.aiAnalysisSummary}"
            </p>
          </div>

          {/* Strengths Card */}
          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-3">
            <div className="flex items-center gap-2 text-emerald-400 text-xs font-mono font-bold uppercase">
              <CheckCircle2 className="w-4 h-4" />
              <span>Core Strengths Aligned</span>
            </div>
            <ul className="space-y-2 text-xs text-gray-300">
              {opportunity.strengths.map((str, idx) => (
                <li key={idx} className="flex items-start gap-2">
                  <span className="text-emerald-400 font-bold">•</span>
                  <span>{str}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Gaps Card */}
          <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-3">
            <div className="flex items-center gap-2 text-amber-400 text-xs font-mono font-bold uppercase">
              <AlertTriangle className="w-4 h-4" />
              <span>Gaps to Address Before Interview</span>
            </div>
            <ul className="space-y-2 text-xs text-gray-300">
              {opportunity.gaps.map((gap, idx) => (
                <li key={idx} className="flex items-start gap-2">
                  <span className="text-amber-400 font-bold">•</span>
                  <span>{gap}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Direct CTA Box */}
          <div className="p-6 rounded-3xl bg-gradient-to-br from-red-950/40 to-[#121420] border border-red-500/30 text-center space-y-3">
            <div className="font-display font-bold text-white text-base">
              Ready to test your responses?
            </div>
            <p className="text-xs text-gray-400">
              Face simulated technical questions with our interactive AI mentor avatar before speaking to TechNova.
            </p>
            <button
              onClick={handleStartInterview}
              className="w-full flex items-center justify-center gap-2 py-3 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs uppercase tracking-wider transition-all shadow-lg shadow-red-600/30"
            >
              <Bot className="w-4 h-4" />
              <span>Launch 1-on-1 AI Interview</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
