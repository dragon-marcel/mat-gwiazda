import React, { useEffect } from 'react';
import Header from './Header';
import { useAuth } from '../contexts/AuthContext';

type Props = {
  title?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
};

const PageLayout: React.FC<Props> = ({ title, children, className = '' }) => {
  const { user } = useAuth();

  useEffect(() => {
    // if document isn't available (SSR/test env), do nothing
    if (typeof document === 'undefined') return;
    const prev = document.title;
    const newTitle = user ? `MatGwiazda - ${user.userName ?? user.email}` : 'MatGwiazda';
    document.title = newTitle;
    return () => {
      document.title = prev;
    };
  }, [user]);

  return (
    <div className="min-h-screen bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100">
      <div className="max-w-4xl mx-auto p-6">
        <Header />

        <div className={`max-w-3xl mx-auto bg-white dark:bg-slate-800 p-6 rounded-md relative ${className}`}>
          {title ? (
            <div className="mb-4">
              <h1 className="text-2xl font-semibold">{title}</h1>
            </div>
          ) : null}

          {children}
        </div>
      </div>
    </div>
  );
};

export default PageLayout;
