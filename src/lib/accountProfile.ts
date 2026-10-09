import { UserProfile } from '../types/user';

export interface AccountRecord {
  userId: string;
  email: string;
  onboardingCompleted: boolean;
  onboardingCompletedAt: string | null;
  profile: UserProfile | null;
}

export function emptyProfile(userId: string): UserProfile {
  return {
    id: userId,
    name: '',
    age: 0,
    country: '',
    currentLocation: '',
    currentSchool: '',
    educationLevel: '',
    major: '',
    graduationYear: new Date().getFullYear(),
    expectedGraduationDate: '',
    skills: [],
    languages: [],
    experiences: [],
    goal: {
      mode: 'job',
      preferredLocation: '',
    },
    profileCompletion: 0,
  };
}

export function completionScore(profile: UserProfile): number {
  const checks = [
    profile.name.trim(),
    profile.country.trim(),
    profile.currentLocation.trim(),
    profile.currentSchool.trim(),
    profile.major.trim(),
    profile.skills.length > 0,
    profile.languages.length > 0,
    profile.goal.preferredLocation.trim(),
    profile.experiences.length > 0,
    profile.resumeFileName?.trim(),
  ];
  const filled = checks.filter(Boolean).length;
  return Math.round((filled / checks.length) * 100);
}

export function mergeProfile(
  userId: string,
  existing: UserProfile | null,
  patch: Partial<UserProfile>,
): UserProfile {
  const base = existing ?? emptyProfile(userId);
  const next: UserProfile = {
    ...base,
    ...patch,
    id: userId,
    goal: {
      ...base.goal,
      ...(patch.goal ?? {}),
    },
    skills: patch.skills ?? base.skills,
    languages: patch.languages ?? base.languages,
    experiences: patch.experiences ?? base.experiences,
  };
  next.profileCompletion = completionScore(next);
  return next;
}

export function validateOnboardingProfile(profile: UserProfile): string[] {
  const errors: string[] = [];
  if (!profile.name.trim()) errors.push('Name is required.');
  if (!Number.isFinite(profile.age) || profile.age < 15 || profile.age > 100) {
    errors.push('Enter an age between 15 and 100.');
  }
  if (!profile.country.trim()) errors.push('Country is required.');
  if (!profile.currentLocation.trim()) errors.push('Current location is required.');
  if (!profile.currentSchool.trim()) errors.push('School is required.');
  if (!profile.educationLevel.trim()) errors.push('Education level is required.');
  if (!profile.major.trim()) errors.push('Major is required.');
  if (!Number.isFinite(profile.graduationYear) || profile.graduationYear < 2000) {
    errors.push('Graduation year is required.');
  }
  if (profile.skills.length < 1) errors.push('Add at least one skill.');
  if (profile.languages.length < 1) errors.push('Add at least one language.');
  if (!profile.goal.preferredLocation.trim()) errors.push('Preferred location is required.');
  if (profile.goal.mode === 'job' && !profile.goal.desiredRole?.trim()) {
    errors.push('Desired role is required for the career path.');
  }
  if (profile.goal.mode === 'university' && !profile.goal.desiredField?.trim()) {
    errors.push('Desired field is required for the academic path.');
  }
  return errors;
}

export function validateOnboardingStep(
  step: number,
  profile: UserProfile,
): string | null {
  if (step === 1) {
    if (!profile.name.trim()) return 'Name is required.';
    if (!Number.isFinite(profile.age) || profile.age < 15 || profile.age > 100) {
      return 'Enter an age between 15 and 100.';
    }
    if (!profile.country.trim()) return 'Country is required.';
    if (!profile.currentLocation.trim()) return 'Current location is required.';
  }
  if (step === 2) {
    if (!profile.currentSchool.trim()) return 'School is required.';
    if (!profile.educationLevel.trim()) return 'Education level is required.';
    if (!profile.major.trim()) return 'Major is required.';
    if (!Number.isFinite(profile.graduationYear) || profile.graduationYear < 2000) {
      return 'Graduation year is required.';
    }
  }
  if (step === 3 && profile.skills.length < 1) return 'Add at least one skill.';
  if (step === 4 && profile.languages.length < 1) return 'Add at least one language.';
  if (step === 6) {
    if (!profile.goal.preferredLocation.trim()) return 'Preferred location is required.';
    if (profile.goal.mode === 'job' && !profile.goal.desiredRole?.trim()) {
      return 'Desired role is required for the career path.';
    }
    if (profile.goal.mode === 'university' && !profile.goal.desiredField?.trim()) {
      return 'Desired field is required for the academic path.';
    }
  }
  return null;
}
