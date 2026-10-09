export type SkillLevel = 'Beginner' | 'Intermediate' | 'Advanced';

export interface SkillItem {
  id: string;
  name: string;
  level: SkillLevel;
  category?: 'Language' | 'Framework' | 'Database' | 'Cloud' | 'DevOps' | 'Tool';
}

export interface LanguageItem {
  id: string;
  language: string;
  certification?: string; // e.g. JLPT N2, IELTS 7.5, TOEIC 850
  proficiency: 'Basic' | 'Conversational' | 'Professional' | 'Native';
}

export interface ExperienceItem {
  id: string;
  type: 'Project' | 'Internship' | 'Part-time' | 'Certification';
  title: string;
  organization?: string;
  startDate: string;
  endDate?: string;
  description: string;
  technologies?: string[];
}

export type GoalMode = 'job' | 'university';

export interface GoalSettings {
  mode: GoalMode;
  desiredRole?: string;       // for job mode, e.g. "Backend Engineer / System Engineer"
  preferredLocation: string;  // e.g. "Tokyo, Japan"
  preferredIndustry?: string; // e.g. "Fintech / Cloud Infrastructure / Enterprise Tech"
  desiredField?: string;      // for university mode, e.g. "Computer Science & Artificial Intelligence"
  degreeLevel?: 'Bachelor' | 'Master' | 'PhD';
  languagePreference?: 'Japanese' | 'English' | 'Bilingual';
}

export interface UserProfile {
  id: string;
  name: string;
  age: number;
  country: string;
  currentLocation: string;
  currentSchool: string;
  educationLevel: string; // e.g. "IT Vocational School (専門学校)"
  major: string;          // e.g. "Software Engineering & Cloud Systems"
  graduationYear: number; // 2027
  expectedGraduationDate: string; // "March 2027"
  skills: SkillItem[];
  languages: LanguageItem[];
  experiences: ExperienceItem[];
  resumeFileName?: string;
  resumeUploadDate?: string;
  goal: GoalSettings;
  profileCompletion: number; // percentage, e.g. 92
  avatarUrl?: string;
}
