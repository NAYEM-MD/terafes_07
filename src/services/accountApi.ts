import { UserProfile } from '../types/user';
import { AccountRecord } from '../lib/accountProfile';

export class AccountApiError extends Error {
  status: number;
  errors?: string[];

  constructor(message: string, status: number, errors?: string[]) {
    super(message);
    this.name = 'AccountApiError';
    this.status = status;
    this.errors = errors;
  }
}

async function readAccount(response: Response): Promise<AccountRecord> {
  const data = (await response.json().catch(() => ({}))) as Partial<AccountRecord> & {
    error?: string;
    errors?: string[];
  };
  if (!response.ok) {
    throw new AccountApiError(data.error || 'Profile request failed.', response.status, data.errors);
  }
  return {
    userId: data.userId || '',
    email: data.email || '',
    onboardingCompleted: Boolean(data.onboardingCompleted),
    onboardingCompletedAt: data.onboardingCompletedAt ?? null,
    profile: data.profile ?? null,
  };
}

export const accountApi = {
  async getAccount(): Promise<AccountRecord> {
    const response = await fetch('/api/v1/users/me', {
      method: 'GET',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
    });
    return readAccount(response);
  },

  async saveProfile(profile: Partial<UserProfile>, complete: boolean): Promise<AccountRecord> {
    const response = await fetch('/api/v1/users/me', {
      method: 'PUT',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ profile, complete }),
    });
    return readAccount(response);
  },
};
