import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { Button } from '../components/ui/Button';
import Stars from '../components/ui/Stars';

const PlayPage: React.FC = () => {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-slate-900 text-slate-900 dark:text-slate-100">
      <div className="max-w-4xl mx-auto p-6">
        <header className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold">Witaj w MatGwiazda<Stars count={1} inline size="lg"/></h1>
          <div className="flex items-center gap-3">
            <Button variant="ghost" onClick={toggle} aria-pressed={theme === 'dark'}>
              {theme === 'dark' ? '🌙 Dark' : '☀️ Light'}
            </Button>
            {/* Use link-style button for logout like in auth pages */}
            <Button variant="link" size="sm" onClick={logout} className="text-blue-600 dark:text-blue-400">
              Wyloguj
            </Button>
          </div>
        </header>

        <main className="relative bg-white dark:bg-slate-800 rounded-md shadow-md p-6 overflow-hidden">
          {/* Stars in the top-right: number equals user's current level (clamped to 10) */}
          <Stars count={user?.currentLevel ?? 0} className="absolute top-3 right-3" />

          <section className="mb-4">
            <h2 className="text-lg font-semibold">Informacje o koncie</h2>
            <p className="mt-2">Zalogowany jako: <span className="font-medium">{user?.userName || user?.email}</span></p>
            {user?.currentLevel !== undefined && (
              <p className="mt-1">Poziom: <strong>{user.currentLevel}</strong> — Punkty: <strong>{user.points ?? 0}</strong></p>
            )}
          </section>

          <section>
            <h3 className="text-md font-medium mb-2">Rozpocznij grę</h3>
            <p className="text-sm text-muted-foreground">Wybierz zadanie, aby rozpocząć rozwiązywanie i zdobywać gwiazdki.</p>
            {/* Placeholder for play actions */}
            <div className="mt-4">
              {/* Make this the primary CTA similar to register/login main action */}
              <Button size="lg" className="bg-blue-600 text-white hover:bg-blue-700 focus:ring-2 focus:ring-blue-500">Rozpocznij</Button>
            </div>
          </section>
        </main>
      </div>
    </div>
  );
};

export default PlayPage;
