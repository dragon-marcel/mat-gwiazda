import React from 'react';
import type { TaskDto } from '../../../hooks/useTasks';
import { Button } from '../../../components/ui/Button';

type Props = {
  task: TaskDto;
  onClose: () => void;
};

const TaskDetailsDrawer: React.FC<Props> = ({ task, onClose }) => {
  return (
    <div className="fixed inset-0 z-50 flex">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} aria-hidden="true" />
      <aside className="relative ml-auto w-full max-w-2xl bg-white dark:bg-slate-900 h-full shadow-xl p-6 overflow-auto">
        <div className="flex items-start justify-between">
          <h3 className="text-lg font-semibold">Szczegóły zadania</h3>
          <Button variant="ghost" onClick={onClose}>Zamknij</Button>
        </div>

        <div className="mt-4 space-y-4">
          <div>
            <h4 className="text-sm font-medium text-muted-foreground">ID</h4>
            <div className="text-sm">{task.id}</div>
          </div>

          <div>
            <h4 className="text-sm font-medium text-muted-foreground">Poziom</h4>
            <div className="text-sm">{task.level}</div>
          </div>

          <div>
            <h4 className="text-sm font-medium text-muted-foreground">Treść</h4>
            <div className="prose max-w-none text-sm">{task.prompt}</div>
          </div>

          <div>
            <h4 className="text-sm font-medium text-muted-foreground">Opcje</h4>
            {Array.isArray(task.options) && task.options.length > 0 ? (
              <ol className="list-decimal ml-5 space-y-1 text-sm">
                {task.options!.map((o, i) => (
                  <li key={`${i}-${o}`}>{o}{typeof task.correctOptionIndex === 'number' && task.correctOptionIndex === i ? ' ✅' : ''}</li>
                ))}
              </ol>
            ) : (
              <div className="text-sm text-muted-foreground">Brak opcji</div>
            )}
          </div>

          {task.explanation && (
            <div>
              <h4 className="text-sm font-medium text-muted-foreground">Wyjaśnienie</h4>
              <div className="prose max-w-none text-sm">{task.explanation}</div>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <h4 className="text-sm font-medium text-muted-foreground">Utworzono</h4>
              <div>{task.createdAt ? new Date(task.createdAt).toLocaleString() : '-'}</div>
            </div>
            <div>
              <h4 className="text-sm font-medium text-muted-foreground">Zaktualizowano</h4>
              <div>{task.updatedAt ? new Date(task.updatedAt).toLocaleString() : '-'}</div>
            </div>
            <div>
              <h4 className="text-sm font-medium text-muted-foreground">Autor</h4>
              <div>{task.createdById ?? '-'}</div>
            </div>
            <div>
              <h4 className="text-sm font-medium text-muted-foreground">Aktywne</h4>
              <div>{task.isActive ? 'Tak' : 'Nie'}</div>
            </div>
          </div>
        </div>

        <div className="mt-6">
          <Button onClick={onClose}>Zamknij</Button>
        </div>
      </aside>
    </div>
  );
};

export default TaskDetailsDrawer;

