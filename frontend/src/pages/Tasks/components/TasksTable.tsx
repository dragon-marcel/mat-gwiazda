import React from 'react';
import type { TaskDto, Page } from '../../../hooks/useTasks';
import { Button } from '../../../components/ui/Button';

type Props = {
  page: Page<TaskDto> | null;
  loading?: boolean;
  onOpenTask: (id: string) => void;
};

const truncate = (s?: string, n = 80) => (s && s.length > n ? `${s.slice(0, n)}...` : s || '');

const formatOptions = (opts?: string[] | null, maxChars = 100) => {
  if (!Array.isArray(opts) || opts.length === 0) return '-';
  const joined = opts.join(' | ');
  return joined.length > maxChars ? `${joined.slice(0, maxChars)}...` : joined;
};

const TasksTable: React.FC<Props> = ({ page, loading, onOpenTask }) => {
  if (loading) return <div className="p-4 text-sm text-muted-foreground">Ładowanie zadań...</div>;
  if (!page || page.content.length === 0) return <div className="p-4 text-sm text-muted-foreground">Brak zadań do wyświetlenia.</div>;

  return (
    <div className="overflow-x-auto">
      {/* Desktop/table view (md+) */}
      <table className="hidden md:table min-w-full divide-y table-fixed">
        <colgroup>
          <col style={{ width: '5rem' }} />
          <col />
          <col style={{ width: '9rem' }} />
        </colgroup>
        <thead className="bg-gray-50 dark:bg-slate-700">
          <tr>
            <th className="px-4 py-2 text-left text-sm">Poziom</th>
            <th className="px-4 py-2 text-left text-sm">Treść zadania</th>
            <th className="px-4 py-2 text-right text-sm">Akcje</th>
          </tr>
        </thead>
        <tbody className="bg-white dark:bg-slate-800 divide-y">
          {page.content.map((t) => (
            <tr key={t.id} className="hover:bg-slate-50 dark:hover:bg-slate-700 align-top">
              <td className="px-4 py-2 text-sm align-top">{t.level}</td>
              <td className="px-4 py-2 text-sm break-words whitespace-normal">{truncate(t.prompt, 200)}</td>
              <td className="px-4 py-2 text-right text-sm">
                <div className="flex justify-end gap-2">
                  <Button size="sm" variant="ghost" onClick={() => onOpenTask(t.id)}>Zobacz</Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Mobile/card view (smaller than md) */}
      <div className="md:hidden space-y-3">
        {page.content.map((t) => (
          <div key={t.id} className="p-4 bg-white dark:bg-slate-800 rounded-lg shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <div className="text-sm text-slate-400">Poziom</div>
              <div className="text-sm font-medium">{t.level}</div>
            </div>
            <div className="mt-2">
              <div className="text-sm text-slate-400">Treść zadania</div>
              <div className="mt-1 text-sm text-white break-words">{t.prompt}</div>
            </div>
            <div className="mt-3 flex justify-end">
              <Button size="sm" variant="ghost" onClick={() => onOpenTask(t.id)}>Zobacz</Button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default TasksTable;
