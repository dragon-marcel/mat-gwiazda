import React, { useEffect, useRef, useState } from 'react';
import { getTask, submitProgress, generateTask } from '../lib/services/playService';
import type { TaskDto, ProgressSubmitResponseDto, TaskWithProgressDto } from '../types/api';
import { Button } from './ui/Button';
import ErrorBanner from './ui/ErrorBanner';
import { useAuth } from '../contexts/AuthContext';
import OptionRow from './OptionRow';
import Stars from './ui/Stars';

// TaskPlayer: displays a single task, handles selection and submit, shows feedback.
const TaskPlayer: React.FC<{ task: TaskDto | null; progressId?: string; userId?: string; onNext?: (result: ProgressSubmitResponseDto) => void; onResult?: (result: ProgressSubmitResponseDto) => void }> = ({ task, progressId, userId, onNext, onResult }) => {
  const [selectedOption, setSelectedOption] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ProgressSubmitResponseDto | null>(null);
  const [correctIndex, setCorrectIndex] = useState<number | null>(null);
  const [explanation, setExplanation] = useState<string | null>(null);
  const startTimeRef = useRef<number | null>(null);

  useEffect(() => {
    setSelectedOption(null);
    setError(null);
    setResult(null);
    setExplanation(null);
    // prefer server-provided correctIndex when present
    if (task && (task as any).correctOptionIndex !== undefined && (task as any).correctOptionIndex !== null) {
      setCorrectIndex((task as any).correctOptionIndex as number);
    } else {
      // best-effort: try to parse prompt for simple arithmetic like "X + Y" to locate correct option
      const prompt = task?.prompt ?? '';
      const nums = (prompt.match(/-?\d+/g) || []).map(Number);
      let computedIndex: number | null = null;
      if (nums.length >= 2) {
        const sum = nums[0] + nums[1];
        const idx = (task?.options || []).findIndex((o) => String(o).trim() === String(sum));
        if (idx >= 0) computedIndex = idx;
      }
      setCorrectIndex(computedIndex);
    }
    startTimeRef.current = Date.now();
    setLoading(false);
  }, [task]);

  if (!task) return <div className="p-4 text-sm text-muted-foreground">Brak aktualnego zadania — poczekaj lub odśwież stronę.</div>;

  const isMultipleChoice = Array.isArray(task.options) && task.options.length > 0;

  const handleSubmit = async () => {
    setError(null);
    if (!userId) {
      setError('Brak identyfikatora użytkownika. Zaloguj się ponownie.');
      return;
    }
    if (!isMultipleChoice) {
      setError('To zadanie nie jest typu single-choice; aktualnie obsługujemy tylko zadania single-choice.');
      return;
    }
    if (selectedOption === null) {
      setError('Wybierz jedną z opcji przed wysłaniem.');
      return;
    }

    setLoading(true);
    try {
      const timeTakenMs = startTimeRef.current ? Date.now() - startTimeRef.current : undefined;
      const payload = {
        progressId: progressId ?? task.id,
        selectedOptionIndex: selectedOption,
        timeTakenMs,
      };

      // debug: log payload being sent
      // eslint-disable-next-line no-console
      console.debug('TaskPlayer.handleSubmit payload:', payload);

      // send progress
      const resp = await submitProgress(userId, payload);
      // debug: response from submit
      // eslint-disable-next-line no-console
      console.debug('TaskPlayer.submitProgress response:', resp);

      // normalize boolean field
      const respAny = resp as unknown as Record<string, any>;
      const normalized: ProgressSubmitResponseDto = { ...(respAny as any), isCorrect: respAny.isCorrect ?? respAny.correct ?? false } as any;
      // debug: normalized result
      // eslint-disable-next-line no-console
      console.debug('TaskPlayer: normalized result:', normalized);
      const normalizedResult = { ...normalized } as ProgressSubmitResponseDto;
      setResult(normalizedResult);
      // reveal explanation from response if present
      setExplanation((normalizedResult as any).explanation ?? null);
      // notify parent that a result is now shown (so parent can avoid auto-generating next task)
      if (onResult) onResult(normalizedResult);

      // IMPORTANT: don't call updateUserFromProgress here — updating user context causes PlayView
      // to regenerate a new task immediately (it listens to `user`), which would clear the feedback.
      // Defer updating the auth context until the user clicks "Następne pytanie" (handled in PlayView).
      // updateUserFromProgress(resp);

      // attempt to fetch full task (may include authoritative correctOptionIndex and explanation)
      // use the freshly normalizedResult (captured) instead of the stale `result` state
      void getTask(task.id).then((full) => {
        if (!full) return;
        if (typeof full.correctOptionIndex === 'number') {
          // eslint-disable-next-line no-console
          console.debug('TaskPlayer: fetched full task with correctIndex:', full.correctOptionIndex);
          setCorrectIndex(full.correctOptionIndex);
        }
        // Only set explanation if we already have a result (we must not reveal explanation before submit)
        if (normalizedResult && (full.explanation !== undefined && full.explanation !== null)) {
          setExplanation(full.explanation ?? null);
        }
      }).catch(() => { /* ignore */ });

    } catch (e: any) {
      setError(e?.message ?? 'Błąd wysyłania odpowiedzi');
    } finally {
      setLoading(false);
    }
  };

  const isCorrectFlag = Boolean((result as any)?.isCorrect ?? (result as any)?.correct ?? false);

  // NOTE: per user request we remove the separate feedback panel and correctness text.
  // Instead we will only render a single "Następne pytanie" button whose color depends on isCorrectFlag.

  return (
    <div className="p-4">
      <h4 className="text-lg font-semibold mb-2">{task.prompt}</h4>
      {/* Explanation is hidden until user submits an answer */}
      {explanation ? (
        <div className="prose max-w-none mb-4 text-sm text-slate-800 dark:text-slate-100">{explanation}</div>
      ) : null}

      {isMultipleChoice ? (
        <div className="space-y-2 mb-4" role="radiogroup" aria-label={`Opcje dla zadania ${task.prompt}`}>
          {task.options!.map((opt, i) => (
            <OptionRow
              key={`${task.id}-${i}-${opt.slice(0, 20).split(/\s+/).join('-')}`}
              opt={opt}
              i={i}
              selectedOption={selectedOption}
              result={result}
              correctIndex={correctIndex}
              disabled={!!result}
              onSelect={(idx) => setSelectedOption(idx)}
            />
          ))}
        </div>
      ) : (
        <div className="mb-4">
          <label htmlFor={`free-${task.id}`} className="block text-sm mb-1">Twoja odpowiedź</label>
          <input
            id={`free-${task.id}`}
            value={''}
            readOnly
            className="w-full rounded-md border p-2 bg-gray-100 dark:bg-slate-700 cursor-not-allowed"
            aria-label="Odpowiedź (tylko single-choice supported)"
          />
        </div>
      )}

      {error && <ErrorBanner message={error} />}

      {result ? (
        // Only render a single Next button; color it green if correct, red if incorrect.
        <div className="mt-4">
          <Button
            size="lg"
            variant="default"
            className={`w-full py-3 shadow-md text-white transition-all ${isCorrectFlag ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'}`}
            onClick={() => {
              // eslint-disable-next-line no-console
              console.debug('TaskPlayer: Next button clicked, result:', result);
              if (onNext) onNext(result as ProgressSubmitResponseDto);
            }}
          >
            Nowe zadanie
          </Button>
        </div>
      ) : (
        <div className="mt-4">
          {/* Make the check button full width and more prominent */}
          <Button
            size="lg"
            variant="default"
            className="w-full py-3 shadow-md bg-indigo-600 hover:bg-indigo-700 text-white transition-all"
            onClick={handleSubmit}
            disabled={loading}
          >
            {loading ? 'Wysyłanie...' : 'Sprawdź odpowiedź'}
          </Button>
        </div>
      )}
    </div>
  );
};

// PlayView: container that generates tasks and shows TaskPlayer
const PlayView: React.FC = () => {
  const [selectedTask, setSelectedTask] = useState<TaskDto | null>(null);
  const [progressId, setProgressId] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [feedbackVisible, setFeedbackVisible] = useState(false);
  const [lastResult, setLastResult] = useState<ProgressSubmitResponseDto | null>(null);
  const { user, updateUserFromProgress } = useAuth();

  // ensure we generate the initial task only once when user becomes available
  const initialGeneratedRef = React.useRef(false);
  useEffect(() => {
    if (initialGeneratedRef.current) return;
    if (!user) return; // wait until we have a user
    if (selectedTask) {
      initialGeneratedRef.current = true; // already have a task, nothing to do
      return;
    }
    if (feedbackVisible) {
      // don't generate while feedback is visible
      // eslint-disable-next-line no-console
      console.debug('PlayView: skipping initial generation because feedbackVisible is true');
      return;
    }
    let mounted = true;
    const init = async () => {
      if (!mounted) return;
      setLoading(true);
      try {
        // debug log: generation reason
        // eslint-disable-next-line no-console
        console.debug('PlayView: generating initial task for user', { userId: user?.id, level: user?.currentLevel, selectedTaskExists: Boolean(selectedTask) });
        const generated = await generateTask(user?.currentLevel ?? 1, user?.id);
        if (!mounted) return;
        // debug before setting
        // eslint-disable-next-line no-console
        console.debug('PlayView: setting selectedTask (initial)', generated?.task?.id ?? null, 'progressId', generated?.progressId ?? null);
        setSelectedTask(generated.task);
        setProgressId(generated.progressId ?? null);
        initialGeneratedRef.current = true;
      } catch (e: any) {
        setError(e?.message ?? 'Nie można wygenerować zadania');
      } finally {
        setLoading(false);
      }
    };
    void init();
    return () => { mounted = false; };
  }, [user, feedbackVisible]);

  // log when selectedTask changes
  useEffect(() => {
    // eslint-disable-next-line no-console
    console.debug('PlayView: selectedTask changed', { id: selectedTask?.id ?? null, feedbackVisible });
  }, [selectedTask, feedbackVisible]);

  const handleNext = async (resp: ProgressSubmitResponseDto) => {
    // debug: handleAnswered invoked
    // eslint-disable-next-line no-console
    console.debug('PlayView.handleNext called, resp:', resp);
    // Only proceed to generate the next task if feedback is currently visible.
    // This prevents accidental advancement when handleAnswered is called unexpectedly.
    if (!feedbackVisible) {
      // eslint-disable-next-line no-console
      console.debug('PlayView.handleNext ignored because feedbackVisible is false');
      return;
    }

    // clear feedbackVisible before generating next task
    setFeedbackVisible(false);
    // also clear any popup state so it won't persist when we advance
    setLastResult(null);

    // generate next task
    try {
      setLoading(true);
      const generated = await generateTask(user?.currentLevel ?? 1, user?.id);
      // debug before setting
      // eslint-disable-next-line no-console
      console.debug('PlayView: setting selectedTask (next)', generated?.task?.id ?? null, 'progressId', generated?.progressId ?? null);
      setSelectedTask(generated.task);
      setProgressId(generated.progressId ?? null);
    } catch (e: any) {
      console.error('Failed to generate new task after submit', e);
      setError(e?.message ?? 'Nie można wygenerować kolejnego zadania');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-4">
      <div className="bg-white dark:bg-slate-800 p-4 rounded-md shadow-sm min-h-[240px]">
        {/* heading removed as requested */}
        {loading && <p className="text-sm text-muted-foreground">Przetwarzanie...</p>}
        {error && <ErrorBanner message={error} />}
        <div>
          <TaskPlayer
            task={selectedTask}
            progressId={progressId ?? undefined}
            userId={user?.id}
            onNext={handleNext}
            onResult={(res) => {
              // store last result and mark feedback visible
              setLastResult(res);
              setFeedbackVisible(true);
              // update auth context immediately so level/points appear right away in the UI
              try { if (typeof updateUserFromProgress === 'function') updateUserFromProgress(res); } catch (e) { /* ignore */ }
            }}
          />
        </div>
      </div>

      {/* Popup: show when a star was awarded (leveledUp === true per user request) */}
      {lastResult && lastResult.leveledUp === true && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" aria-hidden="true" />
          <div role="dialog" aria-modal="true" className="relative bg-white dark:bg-slate-900 rounded-lg p-6 w-[90%] max-w-md mx-4 text-center shadow-lg">
            <div className="flex flex-col items-center">
              {/* Large animated star using shared Stars component */}
              <Stars inline size="lg" variant="gold" className="mb-4" />
              <h2 className="text-xl font-bold mb-2">Gratulacje!</h2>
              <p className="mb-4">Uzyskałeś gwiazdkę!!</p>
              <button
                className="mt-2 inline-flex items-center px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white rounded-md shadow-sm"
                onClick={() => {
                  // close popup
                  setLastResult(null);
                  // NOTE: do NOT clear feedbackVisible here. feedbackVisible controls whether
                  // the "Następne pytanie" action is allowed; closing the popup should not
                  // disable advancing. handleNext will clear feedbackVisible when the user
                  // actually advances to the next question.
                }}
              >
                Zamknij
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PlayView;

