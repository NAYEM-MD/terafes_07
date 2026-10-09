import React, { createContext, useCallback, useContext, useState, useEffect, ReactNode } from 'react';
import { getUser, logout, onAuthChange, User as IdentityUser } from '@netlify/identity';
import { UserProfile, GoalMode } from '../types/user';
import { AccountRecord } from '../lib/accountProfile';
import { roninApi } from '../services/api';
import { accountApi } from '../services/accountApi';

export interface ToastMessage {
  id: string;
  type: 'success' | 'info' | 'error' | 'warning';
  message: string;
}

interface AppContextType {
  user: UserProfile | null;
  isLoadingUser: boolean;
  authReady: boolean;
  authUser: IdentityUser | null;
  isAccount: boolean;
  accountProfile: UserProfile | null;
  onboardingCompleted: boolean;
  goalMode: GoalMode;
  setGoalMode: (mode: GoalMode) => void;
  selectedOpportunityId: string;
  setSelectedOpportunityId: (id: string) => void;
  savedOpportunityIds: string[];
  toggleSaveOpportunity: (id: string) => Promise<void>;
  updateUser: (updated: Partial<UserProfile>) => Promise<void>;
  refreshAccount: () => Promise<AccountRecord | null>;
  saveAccountProfile: (profile: Partial<UserProfile>, complete: boolean) => Promise<AccountRecord>;
  signOut: () => Promise<void>;
  toasts: ToastMessage[];
  addToast: (message: string, type?: ToastMessage['type']) => void;
  removeToast: (id: string) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [previewUser, setPreviewUser] = useState<UserProfile | null>(null);
  const [accountProfile, setAccountProfile] = useState<UserProfile | null>(null);
  const [authUser, setAuthUser] = useState<IdentityUser | null>(null);
  const [onboardingCompleted, setOnboardingCompleted] = useState(false);
  const [isLoadingUser, setIsLoadingUser] = useState(true);
  const [authReady, setAuthReady] = useState(false);
  const [goalMode, setGoalModeState] = useState<GoalMode>('job');
  const [selectedOpportunityId, setSelectedOpportunityId] = useState<string>('job-1');
  const [savedOpportunityIds, setSavedOpportunityIds] = useState<string[]>(['job-1', 'job-4', 'uni-1']);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const applyAccount = useCallback((account: AccountRecord | null) => {
    if (!account) {
      setAccountProfile(null);
      setOnboardingCompleted(false);
      return;
    }
    setAccountProfile(account.profile);
    setOnboardingCompleted(account.onboardingCompleted);
    if (account.profile?.goal?.mode) {
      setGoalModeState(account.profile.goal.mode);
    }
  }, []);

  const refreshAccount = useCallback(async () => {
    const session = await getUser();
    setAuthUser(session);
    if (!session) {
      applyAccount(null);
      return null;
    }
    const account = await accountApi.getAccount();
    applyAccount(account);
    return account;
  }, [applyAccount]);

  const saveAccountProfile = useCallback(async (profile: Partial<UserProfile>, complete: boolean) => {
    const account = await accountApi.saveProfile(profile, complete);
    applyAccount(account);
    return account;
  }, [applyAccount]);

  useEffect(() => {
    let isMounted = true;

    async function boot() {
      let session: IdentityUser | null = null;
      try {
        session = await getUser();
        if (!isMounted) return;
        setAuthUser(session);
        if (session) {
          const account = await accountApi.getAccount();
          if (!isMounted) return;
          applyAccount(account);
        } else {
          const data = await roninApi.getUserProfile();
          if (!isMounted) return;
          setPreviewUser(data);
          setGoalModeState(data.goal.mode);
        }
      } catch (err) {
        console.error('Failed to load account', err);
        if (isMounted && !session) {
          try {
            const data = await roninApi.getUserProfile();
            setPreviewUser(data);
          } catch {
            /* Preview data is optional when storage is down. */
          }
        }
      } finally {
        if (isMounted) {
          setAuthReady(true);
          setIsLoadingUser(false);
        }
      }
    }

    boot();
    const unsubscribe = onAuthChange((_event, session) => {
      setAuthUser(session);
      if (!session) {
        setAccountProfile(null);
        setOnboardingCompleted(false);
      }
    });

    return () => {
      isMounted = false;
      unsubscribe();
    };
  }, [applyAccount]);

  const addToast = (message: string, type: ToastMessage['type'] = 'info') => {
    const id = 'toast-' + Math.random().toString(36).substring(2, 9);
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4000);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const setGoalMode = (mode: GoalMode) => {
    setGoalModeState(mode);
    if (authUser && accountProfile) {
      const goal = { ...accountProfile.goal, mode };
      saveAccountProfile({ goal }, false)
        .then(() => {
          addToast(
            mode === 'job'
              ? 'Switched to Career Path Mode'
              : 'Switched to Academic Path Mode',
            'info',
          );
        })
        .catch(() => {
          addToast('Failed to save path mode', 'error');
        });
      return;
    }
    if (previewUser) {
      const updatedUser = {
        ...previewUser,
        goal: { ...previewUser.goal, mode },
      };
      setPreviewUser(updatedUser);
      roninApi.updateUserProfile({ goal: updatedUser.goal });
      addToast(
        mode === 'job'
          ? 'Switched to Career Path Mode (sample preview)'
          : 'Switched to Academic Path Mode (sample preview)',
        'info',
      );
    }
  };

  const toggleSaveOpportunity = async (id: string) => {
    const isSaved = savedOpportunityIds.includes(id);
    const newSaved = isSaved
      ? savedOpportunityIds.filter((item) => item !== id)
      : [...savedOpportunityIds, id];
    setSavedOpportunityIds(newSaved);
    await roninApi.toggleSaveOpportunity(id);
    addToast(
      isSaved ? 'Removed from saved opportunities' : 'Saved to your target shortlist',
      isSaved ? 'info' : 'success',
    );
  };

  const updateUser = async (updated: Partial<UserProfile>) => {
    try {
      if (authUser) {
        await saveAccountProfile(updated, false);
        addToast('Profile saved to your account', 'success');
        return;
      }
      const saved = await roninApi.updateUserProfile(updated);
      setPreviewUser(saved);
      addToast('Updated in this preview session only', 'success');
    } catch {
      addToast('Failed to save profile changes', 'error');
    }
  };

  const signOut = async () => {
    await logout();
    setAuthUser(null);
    setAccountProfile(null);
    setOnboardingCompleted(false);
    const data = await roninApi.getUserProfile();
    setPreviewUser(data);
    setGoalModeState(data.goal.mode);
  };

  const user = authUser ? accountProfile : previewUser;

  return (
    <AppContext.Provider
      value={{
        user,
        isLoadingUser,
        authReady,
        authUser,
        isAccount: Boolean(authUser),
        accountProfile,
        onboardingCompleted,
        goalMode,
        setGoalMode,
        selectedOpportunityId,
        setSelectedOpportunityId,
        savedOpportunityIds,
        toggleSaveOpportunity,
        updateUser,
        refreshAccount,
        saveAccountProfile,
        signOut,
        toasts,
        addToast,
        removeToast,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
};
