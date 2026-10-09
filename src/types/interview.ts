export type MentorAvatarState = 'idle' | 'listening' | 'thinking' | 'speaking';

export interface InterviewQuestion {
  id: string;
  questionNumber: number;
  totalQuestions: number;
  category: 'Technical' | 'Experience' | 'Problem Solving' | 'Behavioral' | 'Motivation';
  text: string;
  expectedKeypoints: string[];
  suggestedFollowUp?: string;
  difficulty: 'Standard' | 'Challenging' | 'In-depth';
}

export interface TranscriptMessage {
  id: string;
  sender: 'ai' | 'user';
  text: string;
  timestamp: string;
  audioDuration?: string;
  isFollowUp?: boolean;
}

export interface MetricBreakdown {
  category: string;
  score: number; // percentage
  benchmark: number;
  feedback: string;
}

export interface StarMethodFeedback {
  situation: string;
  task: string;
  action: string;
  result: string;
}

export interface InterviewAnalysisResult {
  interviewId: string;
  targetRole: string;
  organization: string;
  overallScore: number; // e.g. 78
  technicalKnowledge: number; // 82
  communication: number; // 70
  problemSolving: number; // 87
  experienceEvidence: number; // 65
  jobFit: number; // 90
  radarData: {
    metric: string;
    score: number;
    benchmark: number;
  }[];
  strongAnswers: string[];
  needsImprovement: string[];
  aiOverallFeedback: string;
  starSuggestion: StarMethodFeedback;
  durationMinutes: number;
  completionDate: string;
}

export interface ActionPlanWeek {
  week: number;
  title: string;
  description: string;
  focusArea: 'Technical' | 'Portfolio' | 'Communication' | 'Applications';
  tasks: {
    id: string;
    text: string;
    completed: boolean;
  }[];
}

export interface FinalReadinessReport {
  id: string;
  candidateName: string;
  targetTitle: string;
  organization: string;
  pathMode: 'job' | 'university';
  overallReadiness: number; // e.g. 84
  generatedDate: string;
  summary: string;
  strongestSkills: string[];
  missingSkills: string[];
  fitScore: number;
  interviewScore: number;
  eligibilityRatio: string; // "5 of 6 listed criteria satisfied"
  fourWeekRoadmap: ActionPlanWeek[];
  recommendedOpportunityIds: string[];
}
