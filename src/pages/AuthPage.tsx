import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AuthError, MissingIdentityError, login, signup, updateUser } from '@netlify/identity';
import { useApp } from '../context/AppContext';
import { MonEmblem } from '../components/common/MonEmblem';
import { GoalMode } from '../types/user';

function authMessage(error: unknown): string {
  if (error instanceof MissingIdentityError) {
    return 'Sign-in is not available until Netlify Identity is enabled for this site. Enable it in Project configuration > Identity, then open the deployed site.';
  }
  if (error instanceof AuthError) {
    if (error.status === 404 || /not found/i.test(error.message)) {
      return 'Sign-in is not available until Netlify Identity is enabled for this site. Enable it in Project configuration > Identity, then use the deployed site.';
    }
    if (error.status === 401) return 'Email or password is incorrect.';
    if (error.status === 403) return 'New signups are turned off for this site.';
    if (error.status === 422) return error.message || 'Check the email and password and try again.';
    return error.message;
  }
  if (error instanceof Error) return error.message;
  return 'Authentication failed.';
}

export const AuthPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { authReady, authUser, refreshAccount, addToast } = useApp();
  const [mode, setMode] = useState<'login' | 'signup'>(searchParams.get('form') === 'signup' ? 'signup' : 'login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nextPassword, setNextPassword] = useState('');
  const [pending, setPending] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const recovery = searchParams.get('recovery') === '1';
  const preferredMode = searchParams.get('mode');
  const goalMode: GoalMode | undefined =
    preferredMode === 'job' || preferredMode === 'university' ? preferredMode : undefined;

  const signedInId = authUser?.id;
  useEffect(() => {
    if (!authReady || !signedInId || recovery) return;
    let cancelled = false;
    refreshAccount()
      .then((account) => {
        if (cancelled || !account) return;
        navigate(account.onboardingCompleted ? '/dashboard' : goalMode ? `/onboarding?mode=${goalMode}` : '/onboarding', {
          replace: true,
        });
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setError(error instanceof Error ? error.message : 'Could not load your saved profile.');
      });
    return () => {
      cancelled = true;
    };
  }, [authReady, signedInId, recovery, refreshAccount, navigate, goalMode]);

  const continueAfterAuth = async () => {
    const account = await refreshAccount();
    if (!account) {
      setError('Signed in, but the account session could not be confirmed.');
      return;
    }
    if (account.onboardingCompleted) {
      navigate('/dashboard', { replace: true });
      return;
    }
    navigate(goalMode ? `/onboarding?mode=${goalMode}` : '/onboarding', { replace: true });
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (pending) return;
    setError('');
    setNotice('');
    if (!email.trim() || !password) {
      setError('Email and password are required.');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    setPending(true);
    try {
      if (mode === 'signup') {
        const user = await signup(email.trim(), password, name.trim() ? { full_name: name.trim() } : undefined);
        if (!user.confirmedAt) {
          setNotice('Account created. Confirm the email, then sign in. Onboarding starts after confirmation.');
          setMode('login');
          return;
        }
      } else {
        await login(email.trim(), password);
      }
      await continueAfterAuth();
    } catch (err) {
      setError(authMessage(err));
    } finally {
      setPending(false);
    }
  };

  const handleResetPassword = async (event: React.FormEvent) => {
    event.preventDefault();
    if (pending) return;
    if (nextPassword.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    setPending(true);
    setError('');
    try {
      await updateUser({ password: nextPassword });
      addToast('Password updated', 'success');
      await continueAfterAuth();
    } catch (err) {
      setError(authMessage(err));
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="relative min-h-screen bg-[#090A0F] text-gray-100 flex items-center justify-center px-4 py-12">
      <div className="fixed inset-0 cyber-grid opacity-30 pointer-events-none" />
      <div className="fixed top-12 left-1/2 -translate-x-1/2 w-[700px] h-[300px] bg-red-600/10 blur-[120px] pointer-events-none rounded-full" />
      <div className="relative z-10 w-full max-w-md glass-panel rounded-3xl border border-white/10 p-6 sm:p-8 shadow-2xl">
        <div className="flex items-center gap-3 mb-6">
          <MonEmblem size={36} />
          <div>
            <div className="font-display font-black text-xl text-white">RONIN AI</div>
            <div className="text-[10px] font-mono text-gray-400">ACCOUNT ACCESS</div>
          </div>
        </div>

        <h1 className="font-display text-2xl text-white font-bold">
          {recovery ? 'Set a new password' : mode === 'login' ? 'Sign in' : 'Create your account'}
        </h1>
        <p className="text-sm text-gray-400 mt-2 mb-6">
          Onboarding is saved to your account and is shown only once after it is stored successfully.
        </p>

        {error && (
          <div role="alert" className="mb-4 rounded-xl border border-red-500/40 bg-red-950/40 px-3 py-2 text-sm text-red-200">
            {error}
          </div>
        )}
        {notice && (
          <div role="status" className="mb-4 rounded-xl border border-emerald-500/30 bg-emerald-950/40 px-3 py-2 text-sm text-emerald-200">
            {notice}
          </div>
        )}

        {recovery ? (
          <form onSubmit={handleResetPassword} className="space-y-4">
            <label className="block text-xs font-mono text-gray-300">
              NEW PASSWORD
              <input
                type="password"
                autoComplete="new-password"
                value={nextPassword}
                onChange={(event) => setNextPassword(event.target.value)}
                className="mt-2 w-full min-h-11 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
              />
            </label>
            <button
              type="submit"
              disabled={pending}
              className="w-full min-h-12 rounded-xl bg-red-600 hover:bg-red-500 disabled:opacity-60 text-white font-bold text-sm"
            >
              {pending ? 'Saving password...' : 'Update password'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === 'signup' && (
              <label className="block text-xs font-mono text-gray-300">
                NAME
                <input
                  type="text"
                  autoComplete="name"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  className="mt-2 w-full min-h-11 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
                />
              </label>
            )}
            <label className="block text-xs font-mono text-gray-300">
              EMAIL
              <input
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                className="mt-2 w-full min-h-11 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
              />
            </label>
            <label className="block text-xs font-mono text-gray-300">
              PASSWORD
              <input
                type="password"
                required
                autoComplete={mode === 'signup' ? 'new-password' : 'current-password'}
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                className="mt-2 w-full min-h-11 px-4 py-3 rounded-xl bg-white/5 border border-white/10 text-white focus:outline-none focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400"
              />
            </label>
            <button
              type="submit"
              disabled={pending}
              className="w-full min-h-12 rounded-xl bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 disabled:opacity-60 text-white font-bold text-sm tracking-wide"
            >
              {pending ? 'Checking account...' : mode === 'login' ? 'Sign in and continue' : 'Create account'}
            </button>
          </form>
        )}

        {!recovery && (
          <button
            type="button"
            onClick={() => {
              setMode(mode === 'login' ? 'signup' : 'login');
              setError('');
              setNotice('');
            }}
            className="mt-4 text-sm text-gray-300 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-red-400 rounded-md"
          >
            {mode === 'login' ? 'Need an account? Create one' : 'Already registered? Sign in'}
          </button>
        )}
      </div>
    </div>
  );
};
