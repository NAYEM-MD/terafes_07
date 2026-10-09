import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { handleAuthCallback } from '@netlify/identity';
import { accountApi } from '../../services/accountApi';

export function AuthCallbackRedirect() {
  const navigate = useNavigate();
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;

    handleAuthCallback()
      .then(async (result) => {
        if (!result) return;
        if (result.type === 'recovery') {
          navigate('/login?recovery=1', { replace: true });
          return;
        }
        if (result.type === 'invite') return;
        if (!result.user) return;
        const account = await accountApi.getAccount();
        navigate(account.onboardingCompleted ? '/dashboard' : '/onboarding', { replace: true });
      })
      .catch((error) => {
        console.error('Auth callback failed', error);
      });
  }, [navigate]);

  return null;
}
