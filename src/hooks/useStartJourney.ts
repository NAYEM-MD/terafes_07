import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { GoalMode } from '../types/user';
import { useApp } from '../context/AppContext';

export function useStartJourney() {
  const navigate = useNavigate();
  const { authReady, authUser, refreshAccount, addToast } = useApp();
  const [pending, setPending] = useState(false);

  const startJourney = async (mode?: GoalMode) => {
    if (!authReady || pending) return;
    setPending(true);
    const params = new URLSearchParams({ intent: 'journey' });
    if (mode) params.set('mode', mode);
    try {
      if (!authUser) {
        navigate(`/login?${params.toString()}`);
        return;
      }
      const account = await refreshAccount();
      if (!account) {
        navigate(`/login?${params.toString()}`);
        return;
      }
      if (account.onboardingCompleted) {
        navigate('/dashboard');
        return;
      }
      navigate(mode ? `/onboarding?mode=${mode}` : '/onboarding');
    } catch (error) {
      addToast(
        error instanceof Error ? error.message : 'Could not check your onboarding status.',
        'error',
      );
    } finally {
      setPending(false);
    }
  };

  return { startJourney, pending, authReady };
}
