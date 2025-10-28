import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';

type Props = {
  initial: { email?: string; userName?: string } | null;
  loading?: boolean;
  onSave: (payload: { userName?: string; password?: string }) => Promise<void>;
};

const ProfileForm: React.FC<Props> = ({ initial, loading, onSave }) => {
  const { user, loading: authLoading } = useAuth();
  const [userName, setUserName] = useState(initial?.userName ?? '');
  // email is sourced primarily from auth context; fallback to initial prop until user loads
  const email = user?.email ?? initial?.email ?? '';
  const [password, setPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setUserName(initial?.userName ?? '');
  }, [initial?.userName]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (userName && (userName.length < 2 || userName.length > 100)) {
      setError('Nazwa użytkownika musi mieć od 2 do 100 znaków');
      return;
    }
    if (password && password.length < 6) {
      setError('Hasło musi mieć co najmniej 6 znaków');
      return;
    }
    setSaving(true);
    try {
      await onSave({ userName: userName || undefined, password: password || undefined });
      setPassword('');
    } catch (e: any) {
      setError(e?.message ?? 'Błąd zapisu');
    } finally {
      setSaving(false);
    }
  };

  // If auth is still loading and we have no initial values, show skeleton placeholders
  const showSkeleton = authLoading && !initial;

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {showSkeleton ? (
        <div className="space-y-4">
          <div>
            <div className="h-9 rounded bg-slate-700 animate-pulse" />
          </div>
          <div>
            <div className="h-9 rounded bg-slate-700 animate-pulse" />
          </div>
          <div>
            <div className="h-9 rounded bg-slate-700 animate-pulse" />
          </div>
          <div>
            <div className="h-10 rounded bg-slate-600 animate-pulse" />
          </div>
        </div>
      ) : (
        <>
          <div>
            <label htmlFor="profile-email" className="block text-sm font-medium text-white">Email</label>
            <div className="mt-1">
              <Input
                id="profile-email"
                value={email}
                readOnly
                disabled
                aria-disabled="true"
                className="bg-slate-700 text-white border-slate-600 opacity-90 cursor-not-allowed"
                aria-label="email"
                title="Email nie może być edytowany"
              />
            </div>
          </div>

          <div>
            <label htmlFor="profile-username" className="block text-sm font-medium text-white">Nazwa użytkownika</label>
            <div className="mt-1">
              <Input id="profile-username" value={userName} onChange={(e) => setUserName(e.target.value)} className="bg-slate-700 text-white border-slate-600" aria-label="userName" />
            </div>
          </div>

          <div>
            <label htmlFor="profile-password" className="block text-sm font-medium text-white">Nowe hasło (opcjonalne)</label>
            <div className="mt-1">
              <Input id="profile-password" value={password} onChange={(e) => setPassword(e.target.value)} type="password" className="bg-slate-700 text-white border-slate-600" aria-label="password" />
            </div>
          </div>

          {error && <div className="text-red-600 text-sm">{error}</div>}

          <div>
            <Button type="submit" size="lg" className="w-full shadow-md bg-indigo-600 text-white" disabled={saving || loading} aria-busy={saving || loading}>
              {saving ? (
                <>
                  <svg className="animate-spin -ml-1 mr-2 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                  </svg>
                  Zapisywanie...
                </>
              ) : (
                'Zapisz zmiany'
              )}
            </Button>
          </div>
        </>
      )}
    </form>
  );
};

export default ProfileForm;
