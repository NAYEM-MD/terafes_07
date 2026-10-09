import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useApp } from '../context/AppContext';
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
import { SkillLevel, SkillItem, LanguageItem, GoalMode } from '../types/user';

export const OnboardingPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user, updateUser, setGoalMode, addToast } = useApp();

  const initialMode = (searchParams.get('mode') as GoalMode) || user?.goal?.mode || 'job';

  // Step state (1 to 6)
  const [currentStep, setCurrentStep] = useState<number>(1);

  // Form State initialized from mock user
  const [personal, setPersonal] = useState({
    name: user?.name || 'Thushan',
    age: user?.age || 22,
    country: user?.country || 'Sri Lanka',
    currentLocation: user?.currentLocation || 'Tokyo, Japan',
  });

  const [education, setEducation] = useState({
    currentSchool: user?.currentSchool || 'Tokyo Information Technology College',
    educationLevel: user?.educationLevel || 'IT Vocational Degree (専門士)',
    major: user?.major || 'Advanced Software Engineering & Cloud Infrastructure',
    graduationYear: user?.graduationYear || 2027,
    expectedGraduationDate: user?.expectedGraduationDate || 'March 2027',
  });

  const [skills, setSkills] = useState<SkillItem[]>(user?.skills || []);
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillLevel, setNewSkillLevel] = useState<SkillLevel>('Intermediate');

  const [languages, setLanguages] = useState<LanguageItem[]>(user?.languages || []);
  const [newLang, setNewLang] = useState({ language: 'Japanese', certification: 'JLPT N2', proficiency: 'Professional' as const });

  const [resumeUploaded, setResumeUploaded] = useState(true);
  const [resumeName, setResumeName] = useState(user?.resumeFileName || 'Thushan_Resume_SoftwareEngineer_JP_EN.pdf');

  const [goal, setGoal] = useState({
    mode: initialMode,
    desiredRole: user?.goal?.desiredRole || 'Java Backend Engineer / System Engineer',
    preferredLocation: user?.goal?.preferredLocation || 'Tokyo, Japan',
    preferredIndustry: user?.goal?.preferredIndustry || 'Fintech & Enterprise Cloud Services',
    desiredField: user?.goal?.desiredField || 'Computer Science & Distributed Systems',
    degreeLevel: user?.goal?.degreeLevel || ('Master' as const),
    languagePreference: user?.goal?.languagePreference || ('Bilingual' as const),
  });

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

  const handleCompleteOnboarding = async () => {
    await updateUser({
      name: personal.name,
      age: Number(personal.age),
      country: personal.country,
      currentLocation: personal.currentLocation,
      currentSchool: education.currentSchool,
      educationLevel: education.educationLevel,
      major: education.major,
      graduationYear: Number(education.graduationYear),
      expectedGraduationDate: education.expectedGraduationDate,
      skills,
      languages,
      resumeFileName: resumeName,
      goal: {
        mode: goal.mode,
        desiredRole: goal.desiredRole,
        preferredLocation: goal.preferredLocation,
        preferredIndustry: goal.preferredIndustry,
        desiredField: goal.desiredField,
        degreeLevel: goal.degreeLevel,
        languagePreference: goal.languagePreference,
      },
    });

    setGoalMode(goal.mode);
    addToast('Profile calibrated successfully! Welcome to Ronin Command Center.', 'success');
    navigate('/dashboard');
  };

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
            const Icon = step.icon;
            const isCompleted = currentStep > step.num;
            const isCurrent = currentStep === step.num;
            return (
              <button
                key={step.num}
                onClick={() => setCurrentStep(step.num)}
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
                    placeholder="e.g. Thushan Silva"
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
                  Upload your CV or portfolio dossier to enable automatic RAG evidence extraction.
                </p>
              </div>

              {/* Resume Upload Dropzone UI (Visual Only) */}
              <div
                onClick={() => setResumeUploaded(true)}
                className="border-2 border-dashed border-red-500/30 hover:border-red-500/60 rounded-2xl p-8 flex flex-col items-center justify-center text-center bg-white/[0.02] hover:bg-red-950/10 transition-all cursor-pointer group"
              >
                <div className="w-14 h-14 rounded-2xl bg-red-600/10 border border-red-500/30 flex items-center justify-center text-red-400 mb-3 group-hover:scale-105 transition-transform">
                  <Upload className="w-7 h-7" />
                </div>
                <div className="text-sm font-bold text-white">
                  Drag and drop your Resume / Portfolio PDF
                </div>
                <div className="text-xs text-gray-400 mt-1">
                  Supports PDF, DOCX, or Markdown up to 15MB
                </div>

                {resumeUploaded && (
                  <div className="mt-4 flex items-center gap-2 px-3 py-1.5 rounded-lg bg-emerald-950/40 border border-emerald-500/30 text-emerald-300 text-xs font-mono">
                    <FileText className="w-4 h-4" />
                    <span>Attached: {resumeName}</span>
                    <CheckCircle2 className="w-4 h-4 text-emerald-400 ml-1" />
                  </div>
                )}
              </div>

              {/* Preloaded Projects and Experiences */}
              <div className="space-y-3">
                <label className="block text-xs font-mono text-gray-400 uppercase">
                  ACTIVE EXPERIENCES & CAPSTONES
                </label>
                <div className="p-4 rounded-xl bg-white/5 border border-white/10 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-white">
                      Distributed Inventory & Order Gateway
                    </span>
                    <span className="text-[10px] font-mono text-red-400 bg-red-950/40 px-2 py-0.5 rounded border border-red-500/20">
                      Capstone Project
                    </span>
                  </div>
                  <p className="text-xs text-gray-400">
                    Spring Boot, PostgreSQL pessimistic locking, Redis cache, and Docker multi-tier deployment.
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-white/5 border border-white/10 space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-bold text-white">
                      Oracle Certified Professional: Java SE 17 Developer
                    </span>
                    <span className="text-[10px] font-mono text-amber-400 bg-amber-950/40 px-2 py-0.5 rounded border border-amber-500/20">
                      Certification
                    </span>
                  </div>
                  <p className="text-xs text-gray-400">
                    Demonstrated mastery of Java concurrency, memory management, and stream APIs.
                  </p>
                </div>
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
                onClick={() => setCurrentStep(currentStep + 1)}
                className="flex items-center gap-2 px-6 py-3 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white text-xs font-bold uppercase tracking-wider transition-all shadow-lg shadow-red-600/30"
              >
                <span>Continue</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={handleCompleteOnboarding}
                className="flex items-center gap-2 px-8 py-3.5 rounded-xl bg-gradient-to-r from-red-600 via-rose-600 to-amber-600 hover:from-red-500 hover:to-amber-500 text-white text-xs font-bold uppercase tracking-widest transition-all shadow-xl shadow-red-600/40 animate-pulse-slow"
              >
                <Sparkles className="w-4 h-4" />
                <span>Analyze My Path</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
