import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import PageLayout from '../components/PageLayout';
import PlayView from '../components/PlayView';

const PlayPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <PageLayout title="Tryb gry">
      <main className="relative bg-white dark:bg-slate-800 rounded-md shadow-md p-6 overflow-hidden">
        {/* Stars are shown in the header; removed from inner container per design */}

        <section className="mb-4">
          <p className="mt-2">To jest panel rozgrywki — tutaj rozwiązujesz zadania. Wybierz odpowiedź, kliknij "Sprawdź odpowiedź" i zdobywaj punkty oraz gwiazdki.</p>
          <div className="mt-3 flex items-center gap-3">
            {user?.currentLevel !== undefined && (
              <span className="text-sm text-muted-foreground">Poziom: <strong className="font-medium">{user.currentLevel}</strong> — Punkty: <strong className="font-medium">{user.points ?? 0}</strong></span>
            )}
          </div>
        </section>

        <section>
          {/* PlayView: główny widok gry */}
          <div className="mt-4">
            <PlayView />
          </div>
        </section>
      </main>
    </PageLayout>
  );
};

export default PlayPage;
