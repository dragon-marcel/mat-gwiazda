import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import Stars from '../../components/ui/Stars';

const StatsPanel: React.FC = () => {
  const { user } = useAuth();

  const level = user?.currentLevel ?? 0;
  const points = user?.points ?? 0;
  const stars = user?.stars ?? 0;

  return (
    <div className="bg-slate-800 p-4 rounded-md shadow-sm text-white">
      <h3 className="text-md font-medium mb-2">Statystyki</h3>
      <div className="text-sm text-white space-y-2">
        <div className="flex items-center justify-between">
          <span>Poziom</span>
          <span className="font-medium">{level}</span>
        </div>
        <div className="flex items-center justify-between">
          <span>Punkty</span>
          <span className="font-medium">{points}</span>
        </div>
        <div className="flex items-center justify-between">
          <span>Gwiazdki</span>
          <div className="flex items-center gap-2">
            <span className="font-medium">{stars}</span>
            <Stars count={Math.min(stars, 10)} size="sm" />
          </div>
        </div>
      </div>
    </div>
  );
};

export default StatsPanel;
