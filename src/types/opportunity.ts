export type OpportunityType = 'job' | 'university';

export type MatchTier = 'Recommended' | 'Strong Match' | 'Possible' | 'Stretch';

export interface RequirementCriterion {
  id: string;
  name: string;
  isSatisfied: boolean;
  notes?: string;
  userEvidence?: string; // e.g. "Java — 3 personal/school projects"
}

export interface Opportunity {
  id: string;
  type: OpportunityType;
  title: string;              // e.g. "Java Backend Engineer" or "Master in Computer Science"
  organization: string;       // e.g. "TechNova Japan" or "University of Tokyo"
  location: string;           // e.g. "Tokyo, Japan"
  industryOrField: string;    // e.g. "Fintech & Cloud Systems" or "Graduate School of Information Science"
  matchScore: number;         // e.g. 92
  matchTier: MatchTier;
  salaryOrTuition: string;    // e.g. "¥4,800,000 - ¥6,500,000 / year" or "¥535,800 / year"
  languageRequirement: string;// e.g. "JLPT N2 or Business Japanese"
  workStyleOrProgramType: string; // e.g. "Hybrid (Tokyo Office + Remote)" or "2-Year Research Master"
  description: string;
  requirements: string[];
  preferredSkills: string[];
  matchedRequirements: string[];
  missingRequirements: string[];
  criteriaList: RequirementCriterion[];
  strengths: string[];
  gaps: string[];
  aiAnalysisSummary: string;
  postedDate?: string;
  saved?: boolean;
}

export interface OpportunityFilters {
  searchTerm: string;
  type: OpportunityType | 'all';
  matchTier: MatchTier | 'all';
  location: string;
  industry: string;
  japaneseLevel: string;
  skill: string;
  minMatchScore: number;
}
