import React, { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { roninApi } from '../services/api';
import { Opportunity, MatchTier } from '../types/opportunity';
import {
  Search,
  SlidersHorizontal,
  Bookmark,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  Briefcase,
  User,
  Sparkles,
  MapPin,
  Building,
} from 'lucide-react';
import { KanjiBadge } from '../components/common/KanjiBadge';

export const DiscoverPage: React.FC = () => {
  const navigate = useNavigate();
  const { setSelectedOpportunityId, toggleSaveOpportunity, savedOpportunityIds } = useApp();

  const [opportunities, setOpportunities] = useState<Opportunity[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // Filters State
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [activeTab, setActiveTab] = useState<MatchTier | 'all'>('all');
  const [locationFilter, setLocationFilter] = useState<string>('');
  const [skillFilter, setSkillFilter] = useState<string>('');
  const [minMatch, setMinMatch] = useState<number>(0);
  const [showFilters, setShowFilters] = useState<boolean>(false);

  useEffect(() => {
    async function fetchOpportunities() {
      setLoading(true);
      const data = await roninApi.searchOpportunities({
        type: 'job',
        searchTerm,
        matchTier: activeTab,
        location: locationFilter,
        skill: skillFilter,
        minMatchScore: minMatch,
      });
      setOpportunities(data);
      setLoading(false);
    }
    fetchOpportunities();
  }, [searchTerm, activeTab, locationFilter, skillFilter, minMatch]);

  const handleSelectOpportunity = (id: string) => {
    setSelectedOpportunityId(id);
    navigate(`/opportunity/${id}`);
  };

  const tabs: { key: MatchTier | 'all'; label: string; count?: number }[] = [
    { key: 'all', label: 'All Targets' },
    { key: 'Recommended', label: 'Recommended' },
    { key: 'Strong Match', label: 'Strong Match (>80%)' },
    { key: 'Possible', label: 'Possible (>70%)' },
    { key: 'Stretch', label: 'Stretch Pathways' },
  ];

  return (
    <div className="space-y-6 text-left">
      {/* Header and Mode Selection */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <KanjiBadge kanji="機軸探索" subtext="Job Radar" variant="crimson" />
            <h1 className="font-display font-black text-2xl sm:text-3xl text-white">
              AiMentor
            </h1>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            Job opportunities matched to your background. AI Interview stays a separate live practice session.
          </p>
        </div>

        <div className="flex items-center p-1 rounded-2xl bg-white/[0.04] border border-white/10 self-start sm:self-auto">
          <div className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold bg-red-600 text-white shadow-md shadow-red-600/30">
            <Briefcase className="w-3.5 h-3.5" />
            <span>Job Opportunities</span>
          </div>
          <NavLink
            to="/profile"
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold text-gray-400 hover:text-white transition-all focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
          >
            <User className="w-3.5 h-3.5" />
            <span>Profile</span>
          </NavLink>
        </div>
      </div>

      {/* Top Search Bar & Filter Controls */}
      <div className="space-y-3">
        <div className="flex items-center gap-3">
          <div className="relative flex-1">
            <Search className="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="What job are you looking for? (e.g. Java, Spring Boot, Microservices, Tokyo)"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-12 pr-4 py-3.5 rounded-2xl bg-[#121522]/90 border border-white/10 text-white placeholder-gray-500 focus:outline-none focus:border-red-500 text-sm shadow-inner"
            />
          </div>

          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`flex items-center gap-2 px-4 py-3.5 rounded-2xl border transition-colors text-xs font-mono font-semibold ${
              showFilters
                ? 'bg-red-950/60 border-red-500 text-white'
                : 'bg-white/5 hover:bg-white/10 border-white/10 text-gray-300'
            }`}
          >
            <SlidersHorizontal className="w-4 h-4" />
            <span className="hidden sm:inline">Filters</span>
          </button>
        </div>

        {/* Expandable Advanced Filters Drawer */}
        {showFilters && (
          <div className="p-4 sm:p-5 rounded-2xl bg-[#141724] border border-white/10 grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-[11px] font-mono text-gray-400 mb-1.5 uppercase">
                LOCATION / REGION
              </label>
              <select
                value={locationFilter}
                onChange={(e) => setLocationFilter(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs font-mono"
              >
                <option value="">All Regions</option>
                <option value="Tokyo">Tokyo Only</option>
                <option value="Kyoto">Kansai / Kyoto</option>
                <option value="Tsukuba">Tsukuba Science City</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-mono text-gray-400 mb-1.5 uppercase">
                KEYWORD OR SKILL FILTER
              </label>
              <input
                type="text"
                placeholder="e.g. Java, Docker, AWS, SQL"
                value={skillFilter}
                onChange={(e) => setSkillFilter(e.target.value)}
                className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
              />
            </div>

            <div>
              <label className="block text-[11px] font-mono text-gray-400 mb-1.5 uppercase">
                MINIMUM MATCH AFFINITY ({minMatch}%)
              </label>
              <input
                type="range"
                min="0"
                max="90"
                step="5"
                value={minMatch}
                onChange={(e) => setMinMatch(Number(e.target.value))}
                className="w-full accent-red-600 cursor-pointer mt-2"
              />
            </div>
          </div>
        )}

        {/* Match Tier Tabs */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1 pt-1 no-scrollbar">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all border ${
                activeTab === tab.key
                  ? 'bg-red-600/20 text-white border-red-500/50 shadow-md shadow-red-950/40'
                  : 'bg-white/[0.02] text-gray-400 hover:text-white border-white/5 hover:bg-white/5'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Results Count & Disclaimer */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between text-xs text-gray-400 font-mono gap-2 pt-2">
        <span>SHOWING {opportunities.length} DISCOVERED TARGETS</span>
        <span className="text-gray-500">
          * Algorithmic evaluation based on candidate CV & portfolio verified parameters.
        </span>
      </div>

      {/* Opportunity Cards Grid */}
      {!loading && opportunities.length === 0 && (
        <div className="rounded-3xl border border-white/10 bg-white/[0.03] p-8 text-center">
          <p className="text-white font-semibold">No job opportunities match these filters.</p>
          <p className="text-sm text-gray-400 mt-2">Clear a filter or search for another skill.</p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {opportunities.map((opp) => {
          const isSaved = savedOpportunityIds.includes(opp.id);
          const satisfiedCount = opp.criteriaList.filter((c) => c.isSatisfied).length;
          const totalCount = opp.criteriaList.length;

          return (
            <div
              key={opp.id}
              className="glass-panel glass-panel-hover rounded-3xl p-6 flex flex-col justify-between border border-white/10 group relative"
            >
              {/* Card Header */}
              <div>
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div>
                    <span className="text-[10px] font-mono text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                      <Building className="w-3 h-3 text-red-500" />
                      {opp.organization}
                    </span>
                    <h3 className="font-display font-bold text-lg text-white group-hover:text-red-400 transition-colors mt-0.5">
                      {opp.title}
                    </h3>
                  </div>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleSaveOpportunity(opp.id);
                    }}
                    className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white border border-white/5 transition-colors"
                  >
                    <Bookmark className={`w-4 h-4 ${isSaved ? 'fill-amber-400 text-amber-400' : ''}`} />
                  </button>
                </div>

                <div className="flex items-center gap-3 text-xs text-gray-300 font-mono mb-4">
                  <span className="flex items-center gap-1 text-gray-400">
                    <MapPin className="w-3 h-3" />
                    {opp.location}
                  </span>
                  <span>•</span>
                  <span className="text-amber-400">{opp.salaryOrTuition.split('(')[0]}</span>
                </div>

                {/* Match Score Badge */}
                <div className="p-3 rounded-2xl bg-white/[0.03] border border-white/5 mb-4 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Sparkles className="w-4 h-4 text-red-400" />
                    <span className="text-xs font-semibold text-white">Match Affinity</span>
                  </div>
                  <span
                    className={`font-display font-black text-lg ${
                      opp.matchScore >= 90
                        ? 'text-emerald-400'
                        : opp.matchScore >= 80
                        ? 'text-amber-400'
                        : 'text-blue-400'
                    }`}
                  >
                    {opp.matchScore}%
                  </span>
                </div>

                {/* Checklist Breakdown */}
                <div className="space-y-2 mb-4">
                  <div className="text-[11px] font-mono text-gray-400 uppercase">
                    REQUIREMENTS CHECKLIST:
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {opp.criteriaList.map((crit) => (
                      <span
                        key={crit.id}
                        className={`text-[11px] px-2 py-0.5 rounded-md font-sans flex items-center gap-1 ${
                          crit.isSatisfied
                            ? 'bg-emerald-950/40 text-emerald-300 border border-emerald-500/20'
                            : 'bg-amber-950/40 text-amber-300 border border-amber-500/20'
                        }`}
                      >
                        {crit.isSatisfied ? '✅' : '⚠'} {crit.name}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Mandatory Disclaimer from Prompt */}
                <div className="p-2.5 rounded-xl bg-white/[0.02] border border-white/5 text-[11px] text-gray-400 leading-snug">
                  Based on the published requirements, your profile appears to satisfy{' '}
                  <span className="text-white font-semibold">
                    {satisfiedCount} of {totalCount}
                  </span>{' '}
                  listed criteria.
                </div>
              </div>

              {/* Card Action Button */}
              <div className="mt-5 pt-4 border-t border-white/5">
                <button
                  onClick={() => handleSelectOpportunity(opp.id)}
                  className="w-full flex items-center justify-center gap-2 py-2.5 rounded-xl bg-white/5 hover:bg-red-600 text-white font-semibold text-xs transition-all uppercase tracking-wider group-hover:bg-red-600 shadow-md"
                >
                  <span>Analyze Fit</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
