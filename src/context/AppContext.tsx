import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { UserProfile, GoalMode } from '../types/user';
import { roninApi } from '../services/api';

export interface ToastMessage {
  id: string;
  type: 'success' | 'info' | 'error' | 'warning';
  message: string;
}

interface AppContextType {
  user: UserProfile | null;
  isLoadingUser: boolean;
  goalMode: GoalMode;
  setGoalMode: (mode: GoalMode) => void;
  selectedOpportunityId: string;
  setSelectedOpportunityId: (id: string) => void;
  savedOpportunityIds: string[];
  toggleSaveOpportunity: (id: string) => Promise<void>;
  updateUser: (updated: Partial<UserProfile>) => Promise<void>;
  toasts: ToastMessage[];
  addToast: (message: string, type?: ToastMessage['type']) => void;
  removeToast: (id: string) => void;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoadingUser, setIsLoadingUser] = useState<boolean>(true);
  const [goalMode, setGoalModeState] = useState<GoalMode>('job');
  const [selectedOpportunityId, setSelectedOpportunityId] = useState<string>('job-1');
  const [savedOpportunityIds, setSavedOpportunityIds] = useState<string[]>(['job-1', 'job-4', 'uni-1']);
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  useEffect(() => {
    let isMounted = true;
    async function loadInitialProfile() {
      try {
        setIsLoadingUser(true);
        const data = await roninApi.getUserProfile();
        if (isMounted) {
          setUser(data);
          setGoalModeState(data.goal.mode);
        }
      } catch (err) {
        console.error('Failed to load profile', err);
      } finally {
        if (isMounted) setIsLoadingUser(false);
      }
    }
    loadInitialProfile();
    return () => {
      isMounted = false;
    };
  }, []);

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
    if (user) {
      const updatedUser = {
        ...user,
        goal: {
          ...user.goal,
          mode,
        },
      };
      setUser(updatedUser);
      roninApi.updateUserProfile({ goal: updatedUser.goal });
      addToast(
        mode === 'job'
          ? 'Switched to Career Path Mode (💼 Jobs)'
          : 'Switched to Academic Path Mode (🎓 Universities)',
        'info'
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
      isSaved ? 'info' : 'success'
    );
  };

  const updateUser = async (updated: Partial<UserProfile>) => {
    try {
      const saved = await roninApi.updateUserProfile(updated);
      setUser(saved);
      addToast('Profile updated successfully', 'success');
    } catch {
      addToast('Failed to save profile changes', 'error');
    }
  };

  return (
    <AppContext.Provider
      value={{
        user,
        isLoadingUser,
        goalMode,
        setGoalMode,
        selectedOpportunityId,
        setSelectedOpportunityId,
        savedOpportunityIds,
        toggleSaveOpportunity,
        updateUser,
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
