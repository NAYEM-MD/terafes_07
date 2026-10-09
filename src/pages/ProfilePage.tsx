import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  User,
  GraduationCap,
  Wrench,
  Languages,
  Briefcase,
  Award,
  Target,
  Edit3,
  Plus,
  Trash2,
  Check,
  X,
  FileText,
  MapPin,
  Calendar,
  Sparkles,
} from 'lucide-react';
import { KanjiBadge } from '../components/common/KanjiBadge';
import { SkillItem, SkillLevel, LanguageItem } from '../types/user';

export const ProfilePage: React.FC = () => {
  const { user, updateUser, addToast } = useApp();

  const [isEditingPersonal, setIsEditingPersonal] = useState(false);
  const [personalForm, setPersonalForm] = useState({
    name: user?.name || 'Thushan',
    age: user?.age || 22,
    country: user?.country || 'Sri Lanka',
    currentLocation: user?.currentLocation || 'Tokyo, Japan',
  });

  const [isEditingEdu, setIsEditingEdu] = useState(false);
  const [eduForm, setEduForm] = useState({
    currentSchool: user?.currentSchool || '',
    educationLevel: user?.educationLevel || '',
    major: user?.major || '',
    graduationYear: user?.graduationYear || 2027,
    expectedGraduationDate: user?.expectedGraduationDate || '',
  });

  // Skills adding state
  const [newSkillName, setNewSkillName] = useState('');
  const [newSkillLevel, setNewSkillLevel] = useState<SkillLevel>('Intermediate');

  // Languages adding state
  const [newLangName, setNewLangName] = useState('');
  const [newLangCert, setNewLangCert] = useState('');

  if (!user) return null;

  const handleSavePersonal = async () => {
    await updateUser({
      name: personalForm.name,
      age: Number(personalForm.age),
      country: personalForm.country,
      currentLocation: personalForm.currentLocation,
    });
    setIsEditingPersonal(false);
  };

  const handleSaveEducation = async () => {
    await updateUser({
      currentSchool: eduForm.currentSchool,
      educationLevel: eduForm.educationLevel,
      major: eduForm.major,
      graduationYear: Number(eduForm.graduationYear),
      expectedGraduationDate: eduForm.expectedGraduationDate,
    });
    setIsEditingEdu(false);
  };

  const handleAddSkill = async () => {
    if (!newSkillName.trim()) return;
    const newSkill: SkillItem = {
      id: 'sk-' + Date.now(),
      name: newSkillName.trim(),
      level: newSkillLevel,
    };
    const updatedSkills = [...user.skills, newSkill];
    await updateUser({ skills: updatedSkills });
    setNewSkillName('');
  };

  const handleRemoveSkill = async (id: string) => {
    const updatedSkills = user.skills.filter((s) => s.id !== id);
    await updateUser({ skills: updatedSkills });
  };

  const handleAddLanguage = async () => {
    if (!newLangName.trim()) return;
    const newLang: LanguageItem = {
      id: 'lg-' + Date.now(),
      language: newLangName.trim(),
      certification: newLangCert.trim() || undefined,
      proficiency: 'Professional',
    };
    const updated = [...user.languages, newLang];
    await updateUser({ languages: updated });
    setNewLangName('');
    setNewLangCert('');
  };

  const handleRemoveLanguage = async (id: string) => {
    const updated = user.languages.filter((l) => l.id !== id);
    await updateUser({ languages: updated });
  };

  return (
    <div className="space-y-8 text-left">
      {/* Profile Header Banner */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/10 relative overflow-hidden shadow-2xl">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
          <div className="flex items-center gap-5">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-tr from-red-600 via-rose-700 to-amber-600 flex items-center justify-center font-display font-black text-3xl text-white shadow-xl shadow-red-950/50">
              {user.name.charAt(0)}
            </div>

            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <KanjiBadge kanji="武士録" subtext="Candidate Dossier" variant="crimson" />
                <span className="text-xs font-mono text-emerald-400 flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
                  VERIFIED PROFILE
                </span>
              </div>
              <h1 className="font-display font-black text-2xl sm:text-3xl text-white">
                {user.name}
              </h1>
              <p className="text-xs font-mono text-gray-400 flex items-center gap-2">
                <MapPin className="w-3.5 h-3.5 text-red-500" />
                <span>{user.currentLocation}</span>
                <span>•</span>
                <span>{user.currentSchool}</span>
              </p>
            </div>
          </div>

          {/* Profile Completion Bar */}
          <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/5 sm:w-64 space-y-2">
            <div className="flex items-center justify-between text-xs font-mono">
              <span className="text-gray-400">PROFILE COMPLETION</span>
              <span className="text-emerald-400 font-bold">{user.profileCompletion}%</span>
            </div>
            <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden">
              <div
                className="bg-gradient-to-r from-red-600 to-emerald-400 h-full rounded-full transition-all duration-1000"
                style={{ width: `${user.profileCompletion}%` }}
              />
            </div>
            <p className="text-[10px] text-gray-500 font-mono">
              Ready for RAG semantic matching across Tokyo positions
            </p>
          </div>
        </div>
      </div>

      {/* Grid: Personal Details & Education */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Section: Personal Info */}
        <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-white font-display font-bold text-base">
              <User className="w-4 h-4 text-red-500" />
              <span>Personal Particulars</span>
            </div>
            <button
              onClick={() => setIsEditingPersonal(!isEditingPersonal)}
              className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
            >
              <Edit3 className="w-4 h-4" />
            </button>
          </div>

          {!isEditingPersonal ? (
            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">FULL NAME</span>
                <span className="text-white font-semibold">{user.name}</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">AGE</span>
                <span className="text-white font-semibold">{user.age} Years</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">ORIGIN</span>
                <span className="text-white font-semibold">{user.country}</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">CURRENT RESIDENCE</span>
                <span className="text-white font-semibold">{user.currentLocation}</span>
              </div>
            </div>
          ) : (
            <div className="space-y-3 pt-2">
              <div>
                <label className="text-[11px] font-mono text-gray-400">NAME</label>
                <input
                  type="text"
                  value={personalForm.name}
                  onChange={(e) => setPersonalForm({ ...personalForm, name: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs mt-1"
                />
              </div>
              <div>
                <label className="text-[11px] font-mono text-gray-400">CURRENT LOCATION</label>
                <input
                  type="text"
                  value={personalForm.currentLocation}
                  onChange={(e) => setPersonalForm({ ...personalForm, currentLocation: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs mt-1"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  onClick={() => setIsEditingPersonal(false)}
                  className="px-3 py-1.5 rounded-lg bg-white/5 text-gray-400 text-xs"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSavePersonal}
                  className="px-4 py-1.5 rounded-lg bg-red-600 text-white text-xs font-bold"
                >
                  Save
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Section: Education */}
        <div className="glass-panel p-6 rounded-3xl border border-white/10 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-white font-display font-bold text-base">
              <GraduationCap className="w-4 h-4 text-amber-400" />
              <span>Academic Curriculum</span>
            </div>
            <button
              onClick={() => setIsEditingEdu(!isEditingEdu)}
              className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
            >
              <Edit3 className="w-4 h-4" />
            </button>
          </div>

          {!isEditingEdu ? (
            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">INSTITUTION</span>
                <span className="text-white font-semibold">{user.currentSchool}</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">DEGREE TRACK</span>
                <span className="text-white font-semibold">{user.educationLevel}</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">MAJOR</span>
                <span className="text-white font-semibold">{user.major}</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-white/5">
                <span className="text-gray-400 font-mono">SCHEDULED GRADUATION</span>
                <span className="text-emerald-400 font-mono font-semibold">
                  {user.expectedGraduationDate} ({user.graduationYear})
                </span>
              </div>
            </div>
          ) : (
            <div className="space-y-3 pt-2">
              <div>
                <label className="text-[11px] font-mono text-gray-400">INSTITUTION</label>
                <input
                  type="text"
                  value={eduForm.currentSchool}
                  onChange={(e) => setEduForm({ ...eduForm, currentSchool: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs mt-1"
                />
              </div>
              <div>
                <label className="text-[11px] font-mono text-gray-400">MAJOR</label>
                <input
                  type="text"
                  value={eduForm.major}
                  onChange={(e) => setEduForm({ ...eduForm, major: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-white text-xs mt-1"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  onClick={() => setIsEditingEdu(false)}
                  className="px-3 py-1.5 rounded-lg bg-white/5 text-gray-400 text-xs"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSaveEducation}
                  className="px-4 py-1.5 rounded-lg bg-red-600 text-white text-xs font-bold"
                >
                  Save
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Skills Arsenal with Chips */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/10 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-white font-display font-bold text-lg">
            <Wrench className="w-5 h-5 text-red-500" />
            <span>Technical Skills Arsenal</span>
          </div>

          {/* Add Skill mini-form */}
          <div className="flex items-center gap-2">
            <input
              type="text"
              placeholder="Add skill (e.g. AWS, Redis)..."
              value={newSkillName}
              onChange={(e) => setNewSkillName(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
            />
            <select
              value={newSkillLevel}
              onChange={(e) => setNewSkillLevel(e.target.value as SkillLevel)}
              className="px-2 py-1.5 rounded-xl bg-[#141722] border border-white/10 text-white text-xs font-mono"
            >
              <option value="Beginner">Beginner</option>
              <option value="Intermediate">Intermediate</option>
              <option value="Advanced">Advanced</option>
            </select>
            <button
              onClick={handleAddSkill}
              className="p-2 rounded-xl bg-red-600 hover:bg-red-500 text-white transition-colors"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Chips list */}
        <div className="flex flex-wrap gap-2.5">
          {user.skills.map((s) => (
            <div
              key={s.id}
              className="flex items-center gap-2 px-3 py-2 rounded-xl bg-white/[0.03] border border-white/10 text-xs text-gray-200 hover:border-red-500/40 transition-colors"
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

      {/* Languages & Certifications */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/10 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-2 text-white font-display font-bold text-lg">
            <Languages className="w-5 h-5 text-amber-400" />
            <span>Language Certifications & Fluency</span>
          </div>

          <div className="flex items-center gap-2">
            <input
              type="text"
              placeholder="Language (e.g. French)"
              value={newLangName}
              onChange={(e) => setNewLangName(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
            />
            <input
              type="text"
              placeholder="Cert (e.g. DELF B2)"
              value={newLangCert}
              onChange={(e) => setNewLangCert(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-white/5 border border-white/10 text-white text-xs"
            />
            <button
              onClick={handleAddLanguage}
              className="p-2 rounded-xl bg-amber-600 hover:bg-amber-500 text-white transition-colors"
            >
              <Plus className="w-4 h-4" />
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {user.languages.map((l) => (
            <div
              key={l.id}
              className="p-4 rounded-2xl bg-white/[0.03] border border-white/10 flex items-center justify-between"
            >
              <div>
                <div className="text-sm font-bold text-white">{l.language}</div>
                <div className="text-xs text-amber-400 font-mono mt-0.5">
                  {l.certification || l.proficiency}
                </div>
              </div>
              <button
                onClick={() => handleRemoveLanguage(l.id)}
                className="text-gray-500 hover:text-red-400 transition-colors"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Projects & Experiences */}
      <div className="glass-panel p-6 sm:p-8 rounded-3xl border border-white/10 space-y-4">
        <div className="flex items-center gap-2 text-white font-display font-bold text-lg">
          <Briefcase className="w-5 h-5 text-red-500" />
          <span>Projects & Practical Experience</span>
        </div>

        <div className="space-y-4">
          {user.experiences.map((exp) => (
            <div
              key={exp.id}
              className="p-5 rounded-2xl bg-white/[0.02] border border-white/5 space-y-2 text-left"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-white text-base">{exp.title}</span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-red-950/40 text-red-400 border border-red-500/20">
                      {exp.type}
                    </span>
                  </div>
                  <div className="text-xs text-gray-400 font-mono mt-0.5">
                    {exp.organization} • {exp.startDate} {exp.endDate ? `to ${exp.endDate}` : ''}
                  </div>
                </div>
              </div>

              <p className="text-xs text-gray-300 leading-relaxed font-sans">{exp.description}</p>

              {exp.technologies && (
                <div className="flex flex-wrap gap-1.5 pt-2">
                  {exp.technologies.map((t) => (
                    <span
                      key={t}
                      className="px-2 py-0.5 rounded bg-white/5 text-[11px] font-mono text-gray-400"
                    >
                      {t}
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
