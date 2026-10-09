import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { mergeProfile, validateOnboardingProfile, validateOnboardingStep } from '../lib/accountProfile';
import { AccountApiError } from '../services/accountApi';
import {
  User,
  GraduationCap,
  Wrench,
  Languages,
  Briefcase,
  Target,
  ArrowRight,
  ArrowLeft,
  Plus,
  Trash2,
  Upload,
  CheckCircle2,
  Sparkles,
  FileText,
} from 'lucide-react';
import { MonEmblem } from '../components/common/MonEmblem';
import { KanjiBadge } from '../components/common/KanjiBadge';
import { SkillLevel, SkillItem, LanguageItem, ExperienceItem, GoalMode, UserProfile } from '../types/user';

export const OnboardingPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const {
    authReady,
    authUser,
    isLoadingUser,
    accountProfile,
    onboardingCompleted,
    saveAccountProfile,
    addToast,
  } = useApp();

  const hydrated = useRef(false);
  const saveLock = useRef(false);
  const resumeInputRef = useRef<HTMLInputElement>(null);

  // Step state (1 to 6)
  const [currentStep, setCurrentStep] = useState<number>(1);
  const [formReady, setFormReady] = useState(false);
  const [saving, setSaving] = useState(false);

  const [personal, setPersonal] = useState({
    name: '',
    age: 0,
    country: '',
    currentLocation: '',
  });

  const [education, setEducation] = useState({
    currentSchool: '',
    educationLevel: '',
    major: '',
    graduationYear: new Date().getFullYear(),
    expectedGraduationDate: '',
  });

  const [skills, setSkills] = useState<SkillItem[]>([]);
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillLevel, setNewSkillLevel] = useState<SkillLevel>('Intermediate');

  const [languages, setLanguages] = useState<LanguageItem[]>([]);
  const [newLang, setNewLang] = useState({ language: '', certification: '', proficiency: 'Professional' as const });

  const [experiences, setExperiences] = useState<ExperienceItem[]>([]);
  const [newExperience, setNewExperience] = useState({
    title: '',
    type: 'Project' as ExperienceItem['type'],
    description: '',
  });

  const [resumeUploaded, setResumeUploaded] = useState(false);
  const [resumeName, setResumeName] = useState('');

  const [goal, setGoal] = useState<{
    mode: GoalMode;
    desiredRole: string;
    preferredLocation: string;
    preferredIndustry: string;
    desiredField: string;
    degreeLevel: NonNullable<UserProfile['goal']['degreeLevel']>;
    languagePreference: NonNullable<UserProfile['goal']['languagePreference']>;
  }>({
    mode: 'job',
    desiredRole: '',
    preferredLocation: '',
    preferredIndustry: '',
    desiredField: '',
    degreeLevel: 'Bachelor',
    languagePreference: 'Bilingual',
  });

  const requestedMode = searchParams.get('mode');
  const loginTarget = `/login?intent=journey${
    requestedMode === 'job' || requestedMode === 'university' ? `&mode=${requestedMode}` : ''
  }`;

  useEffect(() => {
    if (!authReady || isLoadingUser || hydrated.current) return;
    if (!authUser) {
      navigate(loginTarget, { replace: true });
      return;
    }
    if (onboardingCompleted) {
      navigate('/dashboard', { replace: true });
      return;
    }

    const source = accountProfile;
    const mode =
      requestedMode === 'job' || requestedMode === 'university'
        ? requestedMode
        : source?.goal.mode || 'job';
    setPersonal({
      name: source?.name || '',
      age: source?.age || 0,
      country: source?.country || '',
      currentLocation: source?.currentLocation || '',
    });
    setEducation({
      currentSchool: source?.currentSchool || '',
      educationLevel: source?.educationLevel || '',
      major: source?.major || '',
      graduationYear: source?.graduationYear || new Date().getFullYear(),
      expectedGraduationDate: source?.expectedGraduationDate || '',
    });
    setSkills(source?.skills || []);
    setLanguages(source?.languages || []);
    setExperiences(source?.experiences || []);
    setResumeUploaded(Boolean(source?.resumeFileName));
    setResumeName(source?.resumeFileName || '');
    setGoal({
      mode,
      desiredRole: source?.goal.desiredRole || '',
      preferredLocation: source?.goal.preferredLocation || '',
      preferredIndustry: source?.goal.preferredIndustry || '',
      desiredField: source?.goal.desiredField || '',
      degreeLevel: source?.goal.degreeLevel || 'Bachelor',
      languagePreference: source?.goal.languagePreference || 'Bilingual',
    });
    hydrated.current = true;
    setFormReady(true);
  }, [
    authReady,
    isLoadingUser,
    authUser,
    onboardingCompleted,
    accountProfile,
    navigate,
    loginTarget,
    requestedMode,
  ]);

  const stepTitles = [
    { num: 1, label: 'Personal', icon: User, kanji: '本人' },
    { num: 2, label: 'Education', icon: GraduationCap, kanji: '学歴' },
    { num: 3, label: 'Skills', icon: Wrench, kanji: '技術' },
    { num: 4, label: 'Languages', icon: Languages, kanji: '語学' },
    { num: 5, label: 'Experience', icon: Briefcase, kanji: '実績' },
    { num: 6, label: 'Goal', icon: Target, kanji: '目標' },
  ];

  const handleAddSkill = () => {
    if (!newSkillName.trim()) return;
    const item: SkillItem = {
      id: 'sk-' + Date.now(),
      name: newSkillName.trim(),
      level: newSkillLevel,
    };
    setSkills([...skills, item]);
    setNewSkillName('');
  };

  const handleRemoveSkill = (id: string) => {
    setSkills(skills.filter((s) => s.id !== id));
  };

  const handleAddLanguage = () => {
    if (!newLang.language.trim()) return;
    const item: LanguageItem = {
      id: 'lang-' + Date.now(),
      language: newLang.language,
      certification: newLang.certification,
      proficiency: newLang.proficiency,
    };
    setLanguages([...languages, item]);
  };

  const handleRemoveLanguage = (id: string) => {
    setLanguages(languages.filter((l) => l.id !== id));
  };

  const buildProfile = (): UserProfile => {
    return mergeProfile(authUser?.id || 'pending', accountProfile, {
      name: personal.name.trim(),
      age: Number(personal.age),
      country: personal.country.trim(),
      currentLocation: personal.currentLocation.trim(),
      currentSchool: education.currentSchool.trim(),
      educationLevel: education.educationLevel.trim(),
      major: education.major.trim(),
      graduationYear: Number(education.graduationYear),
      expectedGraduationDate: education.expectedGraduationDate.trim(),
      skills,
      languages,
      experiences,
      resumeFileName: resumeUploaded ? resumeName : undefined,
      goal: {
        mode: goal.mode,
        desiredRole: goal.desiredRole.trim(),
        preferredLocation: goal.preferredLocation.trim(),
        preferredIndustry: goal.preferredIndustry.trim(),
        desiredField: goal.desiredField.trim(),
        degreeLevel: goal.degreeLevel,
        languagePreference: goal.languagePreference,
      },
    });
  };

  const persistDraft = async (profile: UserProfile) => {
    if (saveLock.current) return false;
    saveLock.current = true;
    setSaving(true);
    try {
      const saved = await saveAccountProfile(profile, false);
      if (saved.onboardingCompleted) {
        navigate('/dashboard', { replace: true });
      }
      return true;
    } catch (error) {
      const message =
        error instanceof AccountApiError
          ? error.message
          : 'Could not save your progress. You are still on this step.';
      addToast(message, 'error');
      return false;
    } finally {
      saveLock.current = false;
      setSaving(false);
    }
  };

  const handleContinue = async () => {
    const profile = buildProfile();
    const stepError = validateOnboardingStep(currentStep, profile);
    if (stepError) {
      addToast(stepError, 'warning');
      return;
    }
    const saved = await persistDraft(profile);
    if (saved) setCurrentStep((step) => Math.min(step + 1, 6));
  };

  const handleAddExperience = () => {
    if (!newExperience.title.trim()) return;
    const item: ExperienceItem = {
      id: 'exp-' + Date.now(),
      type: newExperience.type,
      title: newExperience.title.trim(),
      description: newExperience.description.trim(),
      startDate: new Date().toISOString().slice(0, 7),
    };
    setExperiences([...experiences, item]);
    setNewExperience({ title: '', type: 'Project', description: '' });
  };

  const handleCompleteOnboarding = async () => {
    if (saveLock.current) return;
    const profile = buildProfile();
    const errors = validateOnboardingProfile(profile);
    if (errors.length > 0) {
      addToast(errors[0], 'warning');
      return;
    }

    saveLock.current = true;
    setSaving(true);
    try {
      const saved = await saveAccountProfile(profile, true);
      if (!saved.onboardingCompleted) {
        addToast('Onboarding was not marked complete. Your dashboard was not opened.', 'error');
        return;
      }
      addToast('Profile saved. Welcome to your dashboard.', 'success');
      navigate('/dashboard', { replace: true });
    } catch (error) {
      const message =
        error instanceof AccountApiError
          ? error.message
          : 'Could not save your profile. Onboarding is still incomplete.';
      addToast(message, 'error');
    } finally {
      saveLock.current = false;
      setSaving(false);
    }
  };

  if (!formReady) {
    return (
      <div className="min-h-screen bg-[#090A0F] text-gray-100 flex items-center justify-center font-mono text-sm text-gray-300">
        Checking your account...
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#090A0F] text-gray-100 flex flex-col py-8 px-4 sm:px-6 lg:px-8 relative selection:bg-red-600">
      {/* Background decorations */}
      <div className="fixed inset-0 cyber-grid opacity-30 pointer-events-none" />
      <div className="fixed top-12 left-1/2 -translate-x-1/2 w-[700px] h-[300px] bg-red-600/10 blur-[120px] pointer-events-none rounded-full" />

      {/* Header */}
      <div className="max-w-4xl mx-auto w-full flex items-center justify-between pb-8 border-b border-white/5 relative z-10">
        <div className="flex items-center gap-3">
          <MonEmblem size={36} />
          <div>
            <div className="font-display font-black text-xl text-white">RONIN AI</div>
            <div className="text-[10px] font-mono text-gray-400">ONBOARDING PROTOCOL</div>
          </div>
        </div>

        <KanjiBadge kanji="登録調書" subtext="Dossier Setup" variant="gold" />
      </div>

      {/* Progress Step Indicator */}
      <div className="max-w-4xl mx-auto w-full my-8 relative z-10">
        <div className="grid grid-cols-6 gap-2">
          {stepTitles.map((step) => {
            const isCompleted = currentStep > step.num;
            const isCurrent = currentStep === step.num;
            return (
              <button
                key={step.num}
                type="button"
                disabled={step.num > currentStep}
                onClick={() => {
                  if (step.num <= currentStep) setCurrentStep(step.num);
                }}
                className={`flex flex-col items-center text-center p-2 rounded-xl transition-all border ${
                  isCurrent
                    ? 'bg-red-950/40 border-red-500/50 text-white shadow-lg shadow-red-950/50'
                    : isCompleted
                    ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-300'
                    : 'bg-white/[0.02] border-white/5 text-gray-500'
                }`}
              >
                <div
                  className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-mono font-bold mb-1.5 ${
                    isCurrent
                      ? 'bg-red-600 text-white'
                      : isCompleted
                      ? 'bg-emerald-600 text-white'
                      : 'bg-white/5 text-gray-400'
                  }`}
                >
                  {isCompleted ? <CheckCircle2 className="w-4 h-4" /> : step.num}
                </div>
                <span className="text-[11px] font-semibold hidden sm:inline-block">
                  {step.label}
                </span>
                <span className="text-[9px] font-japanese opacity-70 hidden md:inline-block">
                  {step.kanji}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Wizard Form Body */}
      <div className="max-w-4xl mx-auto w-full flex-1 relative z-10">
        <div className="glass-panel p-6 sm:p-10 rounded-3xl border border-white/10 shadow-2xl relative">
          {/* STEP 1: Personal */}
          {currentStep === 1 && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono text-red-400 uppercase tracking-wider">
                  STEP 1 OF 6
                </span>
                <h2 className="font-display font-bold text-2xl text-white mt-1">
                  Personal Identity
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  We only collect essential career parameters. Sensitive data (passports, religion, exact street address) is never requested.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">
                    FULL NAME / NAME IN KATAKANA
                  </label>
                  <input
                    type="text"
                    value={personal.name}
                    onChange={(e) => setPersonal({ ...personal, name: e.target.value })}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-red-500 font-sans text-sm"
                    placeholder="Your name"
                  />
                </div>

                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">AGE</label>
                  <input
                    type="number"
                    value={personal.age}
                    onChange={(e) => setPersonal({ ...personal, age: Number(e.target.value) })}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-red-500 font-sans text-sm"
                  />
                </div>

                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">
                    COUNTRY OF ORIGIN
                  </label>
                  <input
                    type="text"
                    value={personal.country}
                    onChange={(e) => setPersonal({ ...personal, country: e.target.value })}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-red-500 font-sans text-sm"
                    placeholder="e.g. Sri Lanka"
                  />
                </div>

                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">
                    CURRENT LOCATION IN JAPAN
                  </label>
                  <input
                    type="text"
                    value={personal.currentLocation}
                    onChange={(e) => setPersonal({ ...personal, currentLocation: e.target.value })}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-red-500 font-sans text-sm"
                    placeholder="e.g. Tokyo, Japan"
                  />
                </div>
              </div>
            </div>
          )}

          {/* STEP 2: Education */}
          {currentStep === 2 && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono text-red-400 uppercase tracking-wider">
                  STEP 2 OF 6
                </span>
                <h2 className="font-display font-bold text-2xl text-white mt-1">
                  Academic Background
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  Matches your graduation timeframe against Japanese recruiting calendar (新卒採用) and graduate school enrollment terms.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                <div className="sm:col-span-2">
                  <label className="block text-xs font-mono text-gray-300 mb-2">
                    CURRENT INSTITUTION / SCHOOL NAME
                  </label>
                  <input
                    type="text"
                    value={education.currentSchool}
                    onChange={(e) => setEducation({ ...education, currentSchool: e.target.value })}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-red-500 text-sm"
                    placeholder="e.g. Tokyo Information Technology College"
                  />
                </div>

                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">
                    EDUCATION LEVEL / QUALIFICATION TRACK
                  </label>
                  <select
                    value={education.educationLevel}
                    onChange={(e) => setEducation({ ...education, educationLevel: e.target.value })}
                    className="w-full px-4 py-3 rounded-xl bg-[#141722] border border-white/10 text-white focus:outline-none focus:border-red-500 text-sm"
                  >
                    <option value="IT Vocational Degree (専門士)">IT Vocational Degree (専門士)</option>
                    <option value="Advanced Vocational Degree (高度専門士)">Advanced Vocational Degree (高度専門士)</option>
                    <option value="Bachelor Degree (学士)">Bachelor Degree (学士)</option>
                    <option value="Master Degree (修士)">Master Degree (修士)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">MAJOR / CONCENTRATION</label>
                  <input
                    type="text"
                    value={education.major}
                    onChange={(e) => setEducation({ ...education, major: e.target.value })}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-red-500 text-sm"
                    placeholder="e.g. Software Engineering & Cloud Systems"
                  />
                </div>

                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">
                    GRADUATION YEAR
                  </label>
                  <select
                    value={education.graduationYear}
                    onChange={(e) => setEducation({ ...education, graduationYear: Number(e.target.value) })}
                    className="w-full px-4 py-3 rounded-xl bg-[#141722] border border-white/10 text-white focus:outline-none focus:border-red-500 text-sm"
                  >
                    <option value="2025">2025</option>
                    <option value="2026">2026</option>
                    <option value="2027">2027 (Current)</option>
                    <option value="2028">2028</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-mono text-gray-300 mb-2">
                    EXPECTED GRADUATION MONTH
                  </label>
                  <input
                    type="text"
                    value={education.expectedGraduationDate}
                    onChange={(e) => setEducation({ ...education, expectedGraduationDate: e.target.value })}
                    className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus:border-red-500 text-sm"
                    placeholder="e.g. March 2027"
                  />
                </div>
              </div>
            </div>
          )}

          {/* STEP 3: Skills */}
          {currentStep === 3 && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono text-red-400 uppercase tracking-wider">
                  STEP 3 OF 6
                </span>
                <h2 className="font-display font-bold text-2xl text-white mt-1">
                  Technical Arsenal & Skill Chips
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  Add languages, frameworks, databases, and tools with your current proficiency level.
                </p>
              </div>

              {/* Add Skill Form */}
              <div className="flex flex-wrap items-center gap-3 p-4 rounded-2xl bg-white/[0.03] border border-white/10">
                <input
                  type="text"
                  placeholder="e.g. Java, Spring Boot, React, MySQL, AWS, Docker"
                  value={newSkillName}
                  onChange={(e) => setNewSkillName(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleAddSkill()}
                  className="flex-1 min-w-[200px] px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-red-500"
                />

                <select
                  value={newSkillLevel}
                  onChange={(e) => setNewSkillLevel(e.target.value as SkillLevel)}
                  className="px-3 py-2.5 rounded-xl bg-[#141722] border border-white/10 text-white text-xs font-mono"
                >
                  <option value="Beginner">Beginner</option>
                  <option value="Intermediate">Intermediate</option>
                  <option value="Advanced">Advanced</option>
                </select>

                <button
                  type="button"
                  onClick={handleAddSkill}
                  className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold uppercase tracking-wider transition-colors"
                >
                  <Plus className="w-4 h-4" />
                  <span>Add Chip</span>
                </button>
              </div>

              {/* Skill chips list */}
              <div>
                <label className="block text-xs font-mono text-gray-400 mb-3 uppercase">
                  REGISTERED SKILLS ({skills.length})
                </label>
                <div className="flex flex-wrap gap-2.5">
                  {skills.map((s) => (
                    <div
                      key={s.id}
                      className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 hover:border-red-500/40 text-xs text-gray-200 transition-colors"
                    >
                      <span className="font-semibold text-white">{s.name}</span>
                      <span
                        className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${
                          s.level === 'Advanced'
                            ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-500/30'
                            : s.level === 'Intermediate'
                            ? 'bg-amber-950/60 text-amber-400 border border-amber-500/30'
                            : 'bg-slate-800 text-slate-300'
                        }`}
                      >
                        {s.level}
                      </span>
                      <button
                        onClick={() => handleRemoveSkill(s.id)}
                        className="text-gray-500 hover:text-red-400 transition-colors ml-1"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>

              {/* Quick suggestions */}
              <div className="pt-2">
                <span className="text-[11px] font-mono text-gray-500 block mb-2">
                  POPULAR SUGGESTIONS:
                </span>
                <div className="flex flex-wrap gap-2">
                  {['Java', 'Spring Boot', 'React', 'MySQL', 'PostgreSQL', 'Docker', 'Linux', 'AWS', 'Python'].map((s) => (
                    <button
                      key={s}
                      onClick={() => {
                        if (!skills.some((item) => item.name.toLowerCase() === s.toLowerCase())) {
                          setSkills([...skills, { id: 'sk-' + Date.now() + Math.random(), name: s, level: 'Intermediate' }]);
                        }
                      }}
                      className="px-2.5 py-1 rounded-lg bg-white/[0.02] hover:bg-white/[0.08] border border-white/5 text-[11px] text-gray-400 hover:text-white transition-colors"
                    >
                      + {s}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* STEP 4: Languages */}
          {currentStep === 4 && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono text-red-400 uppercase tracking-wider">
                  STEP 4 OF 6
                </span>
                <h2 className="font-display font-bold text-2xl text-white mt-1">
                  Languages & Certifications
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  Crucial for foreign students and international candidates applying in Japan.
                </p>
              </div>

              {/* Add Language Form */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 rounded-2xl bg-white/[0.03] border border-white/10">
                <input
                  type="text"
                  placeholder="Language (e.g. Japanese, English)"
                  value={newLang.language}
                  onChange={(e) => setNewLang({ ...newLang, language: e.target.value })}
                  className="px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-red-500"
                />

                <input
                  type="text"
                  placeholder="Cert (e.g. JLPT N2, IELTS 7.5, TOEIC 850)"
                  value={newLang.certification}
                  onChange={(e) => setNewLang({ ...newLang, certification: e.target.value })}
                  className="px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-red-500"
                />

                <button
                  type="button"
                  onClick={handleAddLanguage}
                  className="flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold uppercase tracking-wider transition-colors"
                >
                  <Plus className="w-4 h-4" />
                  <span>Add Language</span>
                </button>
              </div>

              {/* Languages List */}
              <div className="space-y-2.5">
                {languages.map((l) => (
                  <div
                    key={l.id}
                    className="flex items-center justify-between p-3.5 rounded-xl bg-white/5 border border-white/10"
                  >
                    <div className="flex items-center gap-3">
                      <Languages className="w-5 h-5 text-amber-400" />
                      <div>
                        <div className="text-sm font-semibold text-white">{l.language}</div>
                        <div className="text-xs text-gray-400">
                          {l.certification ? `Certified: ${l.certification}` : 'No formal exam recorded'}
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-xs font-mono px-2 py-0.5 rounded bg-white/10 text-gray-300">
                        {l.proficiency}
                      </span>
                      <button
                        onClick={() => handleRemoveLanguage(l.id)}
                        className="text-gray-500 hover:text-red-400 transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* STEP 5: Experience & Resume Upload UI */}
          {currentStep === 5 && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono text-red-400 uppercase tracking-wider">
                  STEP 5 OF 6
                </span>
                <h2 className="font-display font-bold text-2xl text-white mt-1">
                  Experience & Resume Upload
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  Add the experiences that belong to this account. The resume file name is stored with your profile; the file itself stays on this device.
                </p>
              </div>

              <input
                ref={resumeInputRef}
                type="file"
                accept=".pdf,.doc,.docx,.md,application/pdf"
                className="sr-only"
                onChange={(event) => {
                  const file = event.target.files?.[0];
                  if (!file) return;
                  if (file.size > 15 * 1024 * 1024) {
                    addToast('Choose a file that is 15MB or smaller.', 'warning');
                    return;
                  }
                  setResumeName(file.name);
                  setResumeUploaded(true);
                }}
              />
              <button
                type="button"
                onClick={() => resumeInputRef.current?.click()}
                className="w-full border-2 border-dashed border-red-500/30 hover:border-red-500/60 rounded-2xl p-8 flex flex-col items-center justify-center text-center bg-white/[0.02] hover:bg-red-950/10 transition-all group focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
              >
                <div className="w-14 h-14 rounded-2xl bg-red-600/10 border border-red-500/30 flex items-center justify-center text-red-400 mb-3 group-hover:scale-105 transition-transform">
                  <Upload className="w-7 h-7" />
                </div>
                <div className="text-sm font-bold text-white">
                  Choose a resume or portfolio file
                </div>
                <div className="text-xs text-gray-400 mt-1">
                  PDF, DOCX, or Markdown up to 15MB
                </div>

                {resumeUploaded && (
                  <div className="mt-4 flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-950/40 border border-emerald-500/30 text-emerald-300 text-xs font-mono">
                    <FileText className="w-4 h-4" />
                    <span>File name saved: {resumeName}</span>
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 ml-1" />
                  </div>
                )}
              </button>

              <div className="space-y-3">
                <label className="block text-xs font-mono text-gray-400 uppercase">
                  Experiences
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <input
                    type="text"
                    value={newExperience.title}
                    onChange={(event) => setNewExperience({ ...newExperience, title: event.target.value })}
                    placeholder="Title"
                    className="px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-red-500"
                  />
                  <select
                    value={newExperience.type}
                    onChange={(event) =>
                      setNewExperience({
                        ...newExperience,
                        type: event.target.value as ExperienceItem['type'],
                      })
                    }
                    className="px-4 py-2.5 rounded-xl bg-[#141722] border border-white/10 text-white text-xs font-mono"
                  >
                    <option value="Project">Project</option>
                    <option value="Internship">Internship</option>
                    <option value="Part-time">Part-time</option>
                    <option value="Certification">Certification</option>
                  </select>
                  <button
                    type="button"
                    onClick={handleAddExperience}
                    className="flex items-center justify-center gap-1.5 px-4 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white text-xs font-bold uppercase tracking-wider"
                  >
                    <Plus className="w-4 h-4" />
                    Add
                  </button>
                </div>
                <textarea
                  value={newExperience.description}
                  onChange={(event) => setNewExperience({ ...newExperience, description: event.target.value })}
                  placeholder="What did you build or learn?"
                  className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-sm focus:outline-none focus:border-red-500"
                />
                {experiences.length === 0 && (
                  <p className="text-xs text-gray-500">No experiences added yet. This step can be left empty.</p>
                )}
                {experiences.map((experience) => (
                  <div key={experience.id} className="p-4 rounded-xl bg-white/5 border border-white/10 space-y-2">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-sm font-bold text-white">{experience.title}</span>
                      <button
                        type="button"
                        onClick={() => setExperiences(experiences.filter((item) => item.id !== experience.id))}
                        className="text-gray-500 hover:text-red-400"
                        aria-label={`Remove ${experience.title}`}
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                    <p className="text-xs text-gray-400">{experience.type}</p>
                    {experience.description && <p className="text-xs text-gray-300">{experience.description}</p>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* STEP 6: Goal Selection & Final CTA */}
          {currentStep === 6 && (
            <div className="space-y-6">
              <div>
                <span className="text-xs font-mono text-red-400 uppercase tracking-wider">
                  STEP 6 OF 6
                </span>
                <h2 className="font-display font-bold text-2xl text-white mt-1">
                  Primary Destination & Goal Calibration
                </h2>
                <p className="text-xs text-gray-400 mt-1">
                  Choose your targeted pathway to activate specialized matching algorithms and interview modules.
                </p>
              </div>

              {/* Large Path Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <button
                  type="button"
                  onClick={() => setGoal({ ...goal, mode: 'job' })}
                  className={`p-6 rounded-2xl text-left border transition-all ${
                    goal.mode === 'job'
                      ? 'bg-red-950/40 border-red-500 text-white shadow-xl shadow-red-950/40 ring-1 ring-red-500'
                      : 'bg-white/[0.02] border-white/10 text-gray-400 hover:border-white/20'
                  }`}
                >
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-3xl">💼</span>
                    <KanjiBadge kanji="就職" variant="crimson" />
                  </div>
                  <div className="font-display font-bold text-lg text-white">Find a Job</div>
                  <p className="text-xs text-gray-400 mt-1">
                    Software engineering, cloud infrastructure, and technical roles in Japan.
                  </p>
                </button>

                <button
                  type="button"
                  onClick={() => setGoal({ ...goal, mode: 'university' })}
                  className={`p-6 rounded-2xl text-left border transition-all ${
                    goal.mode === 'university'
                      ? 'bg-amber-950/40 border-amber-500 text-white shadow-xl shadow-amber-950/40 ring-1 ring-amber-500'
                      : 'bg-white/[0.02] border-white/10 text-gray-400 hover:border-white/20'
                  }`}
                >
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-3xl">🎓</span>
                    <KanjiBadge kanji="進学" variant="gold" />
                  </div>
                  <div className="font-display font-bold text-lg text-white">Find a University</div>
                  <p className="text-xs text-gray-400 mt-1">
                    Master’s degree or university transfer programs in Computer Science and Informatics.
                  </p>
                </button>
              </div>

              {/* Dynamic Inputs Based on Goal Mode */}
              {goal.mode === 'job' ? (
                <div className="space-y-4 pt-4 border-t border-white/5">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-xs font-mono text-gray-300 mb-2">
                        DESIRED ROLE
                      </label>
                      <input
                        type="text"
                        value={goal.desiredRole}
                        onChange={(e) => setGoal({ ...goal, desiredRole: e.target.value })}
                        className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-mono text-gray-300 mb-2">
                        PREFERRED LOCATION
                      </label>
                      <input
                        type="text"
                        value={goal.preferredLocation}
                        onChange={(e) => setGoal({ ...goal, preferredLocation: e.target.value })}
                        className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-mono text-gray-300 mb-2">
                        PREFERRED INDUSTRY
                      </label>
                      <input
                        type="text"
                        value={goal.preferredIndustry}
                        onChange={(e) => setGoal({ ...goal, preferredIndustry: e.target.value })}
                        className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
                      />
                    </div>
                  </div>
                </div>
              ) : (
                <div className="space-y-4 pt-4 border-t border-white/5">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div>
                      <label className="block text-xs font-mono text-gray-300 mb-2">
                        DESIRED FIELD
                      </label>
                      <input
                        type="text"
                        value={goal.desiredField}
                        onChange={(e) => setGoal({ ...goal, desiredField: e.target.value })}
                        className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-mono text-gray-300 mb-2">DEGREE LEVEL</label>
                      <select
                        value={goal.degreeLevel}
                        onChange={(e) => setGoal({ ...goal, degreeLevel: e.target.value as any })}
                        className="w-full px-4 py-2.5 rounded-xl bg-[#141722] border border-white/10 text-white text-xs font-mono"
                      >
                        <option value="Bachelor">Bachelor (編入 Transfer)</option>
                        <option value="Master">Master (大学院修士)</option>
                        <option value="PhD">Doctorate / PhD</option>
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs font-mono text-gray-300 mb-2">
                        LANGUAGE STREAM
                      </label>
                      <select
                        value={goal.languagePreference}
                        onChange={(e) => setGoal({ ...goal, languagePreference: e.target.value as any })}
                        className="w-full px-4 py-2.5 rounded-xl bg-[#141722] border border-white/10 text-white text-xs font-mono"
                      >
                        <option value="Bilingual">Bilingual (English + Japanese)</option>
                        <option value="English">All English Degree Program</option>
                        <option value="Japanese">Japanese Domestic Research Lab</option>
                      </select>
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Navigation Buttons */}
          <div className="mt-10 pt-6 border-t border-white/10 flex items-center justify-between">
            {currentStep > 1 ? (
              <button
                type="button"
                onClick={() => setCurrentStep(currentStep - 1)}
                className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-white/5 hover:bg-white/10 text-gray-300 text-xs font-mono uppercase tracking-wider transition-colors"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>
            ) : (
              <div />
            )}

            {currentStep < 6 ? (
              <button
                type="button"
                onClick={handleContinue}
                disabled={saving}
                className="flex items-center gap-2 min-h-11 px-6 py-3 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 disabled:opacity-60 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-lg shadow-red-600/30 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-200"
              >
                <span>{saving ? 'Saving...' : 'Continue'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={handleCompleteOnboarding}
                disabled={saving}
                className="flex items-center gap-2 min-h-12 px-8 py-3.5 rounded-xl bg-gradient-to-r from-red-600 via-rose-600 to-amber-600 hover:from-red-500 hover:to-amber-500 disabled:opacity-60 text-white text-xs font-bold uppercase tracking-widest transition-all shadow-xl shadow-red-600/40 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amber-200"
              >
                <Sparkles className="w-4 h-4" />
                <span>{saving ? 'Saving profile...' : 'Analyze My Path'}</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
