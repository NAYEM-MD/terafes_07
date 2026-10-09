import { UserProfile } from '../types/user';
import { Opportunity, OpportunityFilters } from '../types/opportunity';
import {
  InterviewAnalysisResult,
  FinalReadinessReport,
  TranscriptMessage,
} from '../types/interview';
import { initialMockUser } from '../data/mockUser';
import { mockJobs } from '../data/mockJobs';
import { mockUniversities } from '../data/mockUniversities';
import {
  mockBackendInterviewQuestions,
  mockInterviewAnalysisResult,
  initialTranscriptHistory,
} from '../data/mockInterviewQuestions';

// In-memory mock database state
let currentProfile: UserProfile = { ...initialMockUser };
let allOpportunities: Opportunity[] = [...mockJobs, ...mockUniversities];
let interviewTranscript: TranscriptMessage[] = [...initialTranscriptHistory];

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Clean API service layer ready for future Spring Boot REST endpoints.
 * Base URL will point to e.g. http://localhost:8080/api/v1
 */
export const roninApi = {
  /**
   * Fetch current authenticated user's profile
   * Future: GET /api/v1/users/me
   */
  async getUserProfile(): Promise<UserProfile> {
    await delay(250);
    return JSON.parse(JSON.stringify(currentProfile));
  },

  /**
   * Update user profile data
   * Future: PUT /api/v1/users/me
   */
  async updateUserProfile(updated: Partial<UserProfile>): Promise<UserProfile> {
    await delay(350);
    currentProfile = {
      ...currentProfile,
      ...updated,
    };
    return JSON.parse(JSON.stringify(currentProfile));
  },

  /**
   * Search and filter opportunities (Jobs & Universities)
   * Future: GET /api/v1/opportunities?query=...
   */
  async searchOpportunities(filters?: Partial<OpportunityFilters>): Promise<Opportunity[]> {
    await delay(300);
    let results = [...allOpportunities];

    if (!filters) return results;

    if (filters.type && filters.type !== 'all') {
      results = results.filter((item) => item.type === filters.type);
    }

    if (filters.matchTier && filters.matchTier !== 'all') {
      results = results.filter((item) => item.matchTier === filters.matchTier);
    }

    if (filters.searchTerm && filters.searchTerm.trim() !== '') {
      const q = filters.searchTerm.toLowerCase().trim();
      results = results.filter(
        (item) =>
          item.title.toLowerCase().includes(q) ||
          item.organization.toLowerCase().includes(q) ||
          item.location.toLowerCase().includes(q) ||
          item.requirements.some((r) => r.toLowerCase().includes(q)) ||
          item.description.toLowerCase().includes(q)
      );
    }

    if (filters.location && filters.location.trim() !== '') {
      const loc = filters.location.toLowerCase();
      results = results.filter((item) => item.location.toLowerCase().includes(loc));
    }

    if (filters.skill && filters.skill.trim() !== '') {
      const sk = filters.skill.toLowerCase();
      results = results.filter(
        (item) =>
          item.requirements.some((r) => r.toLowerCase().includes(sk)) ||
          item.preferredSkills.some((p) => p.toLowerCase().includes(sk))
      );
    }

    if (filters.minMatchScore !== undefined && filters.minMatchScore > 0) {
      results = results.filter((item) => item.matchScore >= (filters.minMatchScore || 0));
    }

    return results;
  },

  /**
   * Get single opportunity by ID with full AI match analysis
   * Future: GET /api/v1/opportunities/{id}
   */
  async getOpportunityById(id: string): Promise<Opportunity | null> {
    await delay(200);
    const found = allOpportunities.find((item) => item.id === id);
    return found ? JSON.parse(JSON.stringify(found)) : null;
  },

  /**
   * Toggle save bookmark for an opportunity
   * Future: POST /api/v1/opportunities/{id}/bookmark
   */
  async toggleSaveOpportunity(id: string): Promise<boolean> {
    await delay(150);
    const item = allOpportunities.find((o) => o.id === id);
    if (item) {
      item.saved = !item.saved;
      return item.saved;
    }
    return false;
  },

  /**
   * Initialize a simulated AI interview session
   * Future: POST /api/v1/interviews/start
   */
  async startInterview(opportunityId?: string) {
    await delay(300);
    const opportunity = allOpportunities.find((o) => o.id === opportunityId) || mockJobs[0];
    return {
      interviewId: 'inv-' + Math.random().toString(36).substring(2, 9),
      targetRole: opportunity.title,
      organization: opportunity.organization,
      questions: mockBackendInterviewQuestions,
      currentQuestionIndex: 2, // Question 3 / 10 active
      transcript: JSON.parse(JSON.stringify(interviewTranscript)),
    };
  },

  /**
   * Submit an answer to the AI mentor
   * Returns AI reaction, feedback and dynamic follow-up question
   * Future: POST /api/v1/interviews/{interviewId}/answers
   */
  async submitInterviewAnswer(
    _interviewId: string,
    questionNumber: number,
    answerText: string
  ): Promise<{
    userMessage: TranscriptMessage;
    aiFollowUpMessage: TranscriptMessage;
    nextQuestionNumber: number;
    isFinished: boolean;
  }> {
    // Simulate AI LLM evaluation latency
    await delay(900);

    const now = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userMsg: TranscriptMessage = {
      id: 'usr-' + Date.now(),
      sender: 'user',
      text: answerText,
      timestamp: now,
    };

    // AI generated follow-up response tailored to answer
    let aiFollowUpText = '';
    if (questionNumber < 10) {
      const nextQ = mockBackendInterviewQuestions.find((q) => q.questionNumber === questionNumber + 1);
      aiFollowUpText = `Thank you for detailing your approach. I noted your focus on system resilience. Let us progress to our next inquiry: ${nextQ?.text || 'How do you coordinate with team stakeholders?'}`;
    } else {
      aiFollowUpText = 'Excellent. That concludes all 10 evaluation modules for this technical interview session. Let us now calculate your readiness metrics.';
    }

    const aiMsg: TranscriptMessage = {
      id: 'ai-' + (Date.now() + 1),
      sender: 'ai',
      text: aiFollowUpText,
      timestamp: now,
      isFollowUp: true,
    };

    interviewTranscript.push(userMsg, aiMsg);

    return {
      userMessage: userMsg,
      aiFollowUpMessage: aiMsg,
      nextQuestionNumber: Math.min(questionNumber + 1, 10),
      isFinished: questionNumber >= 10,
    };
  },

  /**
   * Retrieve interview performance evaluation and radar metrics
   * Future: GET /api/v1/interviews/{interviewId}/analysis
   */
  async getInterviewAnalysis(_interviewId?: string): Promise<InterviewAnalysisResult> {
    await delay(350);
    return JSON.parse(JSON.stringify(mockInterviewAnalysisResult));
  },

  /**
   * Generate final comprehensive readiness report
   * Future: POST /api/v1/reports/generate
   */
  async generateReport(
    _userId: string,
    opportunityId?: string
  ): Promise<FinalReadinessReport> {
    await delay(500);
    const opp = allOpportunities.find((o) => o.id === opportunityId) || mockJobs[0];

    const report: FinalReadinessReport = {
      id: 'rep-' + Math.random().toString(36).substring(2, 9),
      candidateName: currentProfile.name,
      targetTitle: opp.title,
      organization: opp.organization,
      pathMode: opp.type,
      overallReadiness: 84,
      generatedDate: new Date().toLocaleDateString('en-US', {
        month: 'long',
        day: 'numeric',
        year: 'numeric',
      }),
      summary: `Thushan exhibits an outstanding engineering baseline in Java 17, Spring Boot architectural patterns, and relational database transactional integrity. With verified JLPT N2 and IELTS 7.5 credentials, candidate is well-positioned for international and Japanese tech environments. Solidifying AWS infrastructure fundamentals and structuring interview explanations with the STAR framework will bring interview-to-offer rate to the top tier.`,
      strongestSkills: [
        'Core Java 17 & JVM Concurrency',
        'Spring Boot & RESTful Layering',
        'PostgreSQL Pessimistic Locking & Schema Design',
        'Bilingual Technical Competency (JLPT N2 + IELTS 7.5)',
        'Linux Administration & Containerization',
      ],
      missingSkills: [
        'AWS Production Architecture (ECS / S3 / RDS IAM)',
        'Automated CI/CD Pipeline Configuration (GitHub Actions)',
        'STAR Method Metric-driven Interview Answers',
      ],
      fitScore: opp.matchScore,
      interviewScore: 78,
      eligibilityRatio: '5 of 6 listed criteria satisfied',
      recommendedOpportunityIds: [opp.id, 'job-4', 'job-2', 'uni-2'],
      fourWeekRoadmap: [
        {
          week: 1,
          title: 'AWS Cloud Fundamentals & Infrastructure as Code',
          description: 'Deploy Spring Boot and PostgreSQL on AWS using ECS Fargate and RDS to eliminate the primary candidate gap.',
          focusArea: 'Technical',
          tasks: [
            { id: 'w1-1', text: 'Set up free-tier AWS account and deploy Dockerized Spring Boot to AWS ECS Fargate', completed: true },
            { id: 'w1-2', text: 'Configure Amazon RDS PostgreSQL with connection pooling and security groups', completed: false },
            { id: 'w1-3', text: 'Document cloud architecture diagram in GitHub portfolio README', completed: false },
          ],
        },
        {
          week: 2,
          title: 'Refine Portfolio Explanations & Quantitative Impact',
          description: 'Rewrite project descriptions emphasizing quantitative metrics (e.g. latency, concurrency volume, memory reduction).',
          focusArea: 'Portfolio',
          tasks: [
            { id: 'w2-1', text: 'Benchmark inventory microservice with JMeter / k6 under 5,000 requests', completed: false },
            { id: 'w2-2', text: 'Add architectural decision records (ADR) justifying pessimistic locking vs optimistic locking', completed: false },
            { id: 'w2-3', text: 'Clean up GitHub repositories with English & Japanese bilingual documentation', completed: false },
          ],
        },
        {
          week: 3,
          title: 'Japanese Technical Interview Simulation & STAR Framing',
          description: 'Practice answering behavioral and architectural trade-off inquiries using Japanese technical terminology.',
          focusArea: 'Communication',
          tasks: [
            { id: 'w3-1', text: 'Formulate 4 STAR stories for capstone challenges in Japanese (状況, 課題, 行動, 成果)', completed: false },
            { id: 'w3-2', text: 'Practice 3 mock sessions with RONIN AI mentor focusing on communication conciseness', completed: false },
            { id: 'w3-3', text: 'Prepare reverse-interview questions inquiring about TechNova engineering culture', completed: false },
          ],
        },
        {
          week: 4,
          title: 'Formal Application Submission & Interview Pipeline',
          description: 'Submit personalized applications to top-affinity opportunities and follow up with tailored cover letters.',
          focusArea: 'Applications',
          tasks: [
            { id: 'w4-1', text: 'Submit formal application for TechNova Japan Java Backend Engineer opening', completed: false },
            { id: 'w4-2', text: 'Submit secondary application to Mercari Japan FinTech Microservices squad', completed: false },
            { id: 'w4-3', text: 'Review University of Tsukuba Informatics Master Special Screening guidelines as parallel track', completed: false },
          ],
        },
      ],
    };

    return report;
  },
};
