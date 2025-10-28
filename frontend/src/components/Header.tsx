import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import { Button } from './ui/Button';
import Stars from './ui/Stars';

const Header: React.FC = () => {
  const { user, logout } = useAuth();
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const isPlayActive = location.pathname.startsWith('/play') || location.pathname === '/';
  const isProfileActive = location.pathname.startsWith('/profile');
  const isTasksActive = location.pathname.startsWith('/tasks');
  const isAdminActive = location.pathname.startsWith('/admin');

  return (
    <header className="flex items-center justify-between mb-6">
      <div className="flex items-center gap-3">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">MatGwiazda</h1>
          <span className="text-sm font-medium text-slate-600 dark:text-slate-300">- {user?.userName || user?.email}</span>
        </div>
        {/* Stars next to title showing user's stars (clamped to 10) */}
        <div className="ml-2">
          <Stars count={Math.min(Number(user?.stars ?? 0), 10)} size="sm" />
        </div>
      </div>

      <div className="flex items-center gap-3">
        <Button
          variant="default"
          size="sm"
          onClick={() => navigate('/play')}
          className={isPlayActive ? 'ring-2 ring-indigo-600 ring-offset-2 ring-offset-white dark:ring-offset-slate-900 font-semibold' : undefined}
        >
          Graj
        </Button>

        <Button
          variant="default"
          size="sm"
          onClick={() => navigate('/tasks')}
          className={isTasksActive ? 'ring-2 ring-indigo-600 ring-offset-2 ring-offset-white dark:ring-offset-slate-900 font-semibold' : undefined}
        >
          Zadania
        </Button>

        <Button
          variant="default"
          size="sm"
          onClick={() => navigate('/profile')}
          className={isProfileActive ? 'ring-2 ring-indigo-600 ring-offset-2 ring-offset-white dark:ring-offset-slate-900 font-semibold' : undefined}
        >
          Profil
        </Button>

        {/* Admin link is visible only to admins */}
        {user?.role === 'ADMIN' && (
          <Button
            variant="default"
            size="sm"
            onClick={() => navigate('/admin/users')}
            className={isAdminActive ? 'ring-2 ring-indigo-600 ring-offset-2 ring-offset-white dark:ring-offset-slate-900 font-semibold' : undefined}
          >
            Admin
          </Button>
        )}

        <Button variant="ghost" onClick={toggle} aria-pressed={theme === 'dark'}>
          {theme === 'dark' ? '🌙 Dark' : '☀️ Light'}
        </Button>

        <Button variant="link" size="sm" onClick={logout} className="text-blue-600 dark:text-blue-400">
          Wyloguj
        </Button>
      </div>
    </header>
  );
};

export default Header;
