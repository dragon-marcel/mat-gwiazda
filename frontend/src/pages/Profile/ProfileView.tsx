import React, { useEffect, useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { getMe, updateMe, deleteMe } from '../../lib/services/profileService';
import { useAuth } from '../../contexts/AuthContext';
import { useTheme } from '../../contexts/ThemeContext';
import { Button } from '../../components/ui/Button';
import Stars from '../../components/ui/Stars';
import ProfileForm from './ProfileForm';
import StatsPanel from './StatsPanel';
import DangerZone from './DangerZone';
import ErrorBanner from '../../components/ui/ErrorBanner';
import PageLayout from '../../components/PageLayout';

const ProfileView: React.FC = () => {
  const { refreshUser, logout, user } = useAuth();
  const { theme, toggle } = useTheme();
  const [savedMsg, setSavedMsg] = useState<string | null>(null);
  const saveTimerRef = useRef<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [initial, setInitial] = useState<{ email?: string; userName?: string } | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const me = await getMe();
        if (!mounted) return;
        setInitial({ email: me.email, userName: me.userName });
      } catch (e: any) {
        setError(e?.response?.data?.message ?? e?.message ?? 'Nie można pobrać profilu');
      } finally {
        if (mounted) setLoading(false);
      }
    };
    void load();
    return () => { mounted = false; };
  }, []);

  const handleSave = async (payload: { userName?: string; password?: string }) => {
    setError(null);
    setLoading(true);
    try {
      const updated = await updateMe(payload as any);
      // refresh auth user context so header/stats update immediately
      try { await refreshUser(); } catch (err) { /* ignore */ }
      setInitial({ email: updated.email, userName: updated.userName });
      // show success popup
      setSavedMsg('Zapisano zmiany');
      // clear any previous timer
      if (saveTimerRef.current) window.clearTimeout(saveTimerRef.current);
      saveTimerRef.current = window.setTimeout(() => setSavedMsg(null), 3000);
    } catch (e: any) {
      // map field-level errors if provided
      const msg = e?.response?.data?.message ?? e?.message ?? 'Błąd podczas aktualizacji profilu';
      setError(msg);
      throw e;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    return () => {
      if (saveTimerRef.current) window.clearTimeout(saveTimerRef.current);
    };
  }, []);

  const handleDelete = async () => {
    setError(null);
    setLoading(true);
    try {
      await deleteMe();
      // logout and redirect to login/goodbye
      try { logout(); } catch {}
      navigate('/login');
    } catch (e: any) {
      setError(e?.response?.data?.message ?? e?.message ?? 'Błąd podczas usuwania konta');
    } finally {
      setLoading(false);
    }
  };

  return (
    <PageLayout title="Twój profil">
      {/* Success toast shown after saving changes */}
      {savedMsg && (
        <div className="fixed top-5 right-5 z-50">
          <div className="flex items-center gap-3 bg-green-600 text-white px-4 py-3 rounded shadow-lg">
            <span>{savedMsg}</span>
            <button aria-label="Zamknij" onClick={() => setSavedMsg(null)} className="ml-2 text-white opacity-90 hover:opacity-100">✕</button>
          </div>
        </div>
      )}

      {error && <ErrorBanner message={error} />}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="md:col-span-2">
          <div className="p-4 rounded-md shadow-sm bg-white dark:bg-slate-800">
            <h2 className="text-lg font-medium mb-2">Dane konta</h2>
            <ProfileForm initial={initial} loading={loading} onSave={handleSave} />
          </div>
        </div>

        <div className="md:col-span-1 space-y-4">
          <StatsPanel />

          <DangerZone onDelete={handleDelete} />
        </div>
      </div>
    </PageLayout>
  );
};

export default ProfileView;
