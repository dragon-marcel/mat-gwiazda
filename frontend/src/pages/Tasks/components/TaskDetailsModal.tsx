import React, { useEffect, useRef } from 'react';
import type { TaskDto } from '../../../hooks/useTasks';
import { Button } from '../../../components/ui/Button';
import OptionRow from '../../../components/OptionRow';

type Props = {
  task: TaskDto;
  onClose: () => void;
};

const TaskDetailsModal: React.FC<Props> = ({ task, onClose }) => {
  const closeRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    // focus the close button when modal opens
    closeRef.current?.focus();

    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    globalThis.addEventListener('keydown', onKey);
    return () => globalThis.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    // remove default dialog border/outline with border-0 and outline-none
    <dialog open className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-transparent border-0 outline-none" aria-modal="true">
      {/* no overlay dimming — show a dark-mode styled card instead */}
      <div className="relative z-10 w-full max-w-5xl bg-slate-900 text-white rounded-lg shadow-2xl p-6 mx-2 overflow-auto max-h-[90vh]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 className="text-xl font-semibold">Szczegóły zadania</h3>
            <div className="text-sm text-slate-300 mt-1">Poziom: <span className="font-medium text-white">{task.level}</span></div>
          </div>

          <div className="flex items-start gap-2">
            <Button variant="ghost" onClick={onClose} aria-label="Zamknij okno" ref={closeRef}>✕</Button>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-3 gap-6">
          {/* Left: main content - spans 2 cols (largest) */}
          <div className="col-span-2">
            <div>
              <h4 className="text-sm font-medium text-slate-300">Treść zadania</h4>
              <div className="prose max-w-none text-base text-white mt-2">{task.prompt}</div>
            </div>

            {Array.isArray(task.options) && task.options.length > 0 && (
              <div className="mt-4">
                <h4 className="text-sm font-medium text-slate-300">Opcje</h4>
                <div className="mt-2 space-y-2">
                  {task.options!.map((o, i) => (
                    <OptionRow
                      key={`${task.id}-opt-${i}`}
                      opt={o}
                      i={i}
                      selectedOption={null}
                      result={null}
                      correctIndex={typeof task.correctOptionIndex === 'number' ? task.correctOptionIndex : null}
                      disabled={true}
                      onSelect={() => { /* no-op - readonly */ }}
                      revealCorrect={true}
                    />
                  ))}
                </div>
              </div>
            )}

            {task.explanation !== undefined && (
              <div className="mt-4">
                <h4 className="text-sm font-medium text-slate-300">Wyjaśnienie</h4>
                <textarea
                  value={task.explanation ?? ''}
                  readOnly
                  aria-label="Wyjaśnienie zadania"
                  className="w-full bg-slate-800 text-white p-3 mt-2 resize-none min-h-[120px] border border-slate-700 rounded-md focus:outline-none whitespace-pre-wrap leading-6 text-sm"
                />
              </div>
            )}
          </div>

          {/* Right: metadata / small column */}
          <aside className="col-span-1 text-sm text-slate-300 space-y-4">
            <div>
              <h4 className="text-sm font-medium text-slate-300">Zaktualizowano</h4>
              <div className="text-white/90">{task.updatedAt ? new Date(task.updatedAt).toLocaleString() : '-'}</div>
            </div>
            <div>
              <h4 className="text-sm font-medium text-slate-300">Aktywne</h4>
              <div className="text-white/90">{task.isActive ? 'Tak' : 'Nie'}</div>
            </div>
          </aside>
        </div>

        <div className="mt-6 text-right">
          <Button onClick={onClose}>Zamknij</Button>
        </div>
      </div>
    </dialog>
  );
};

export default TaskDetailsModal;
