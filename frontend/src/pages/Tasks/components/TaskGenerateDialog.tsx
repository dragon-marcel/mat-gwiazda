import React, { useState } from 'react';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Label } from '../../../components/ui/label';
import { FormItem } from '../../../components/ui/form';
import { useAuth } from '../../../contexts/AuthContext';

type Props = {
  onGenerate: (level?: number) => Promise<void> | void;
  onClose: () => void;
};

const LEVELS = Array.from({ length: 8 }, (_, i) => i + 1);

const TaskGenerateDialog: React.FC<Props> = ({ onGenerate, onClose }) => {
  const [level, setLevel] = useState<number | ''>('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { user } = useAuth();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (level === '') {
      setError('Wybierz poziom (1-8)');
      return;
    }
    setSubmitting(true);
    try {
      await onGenerate(Number(level));
      onClose();
    } catch (e: any) {
      setError(e?.message ?? 'Błąd generowania');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} aria-hidden="true" />
      <form onSubmit={handleSubmit} className="relative bg-white dark:bg-slate-900 rounded-md p-6 w-[90%] max-w-md shadow-lg">
        <h3 className="text-lg font-semibold mb-3">Generuj zadanie</h3>

        <FormItem className="mb-3">
          <Label className="mb-1">Poziom</Label>
          <select
            value={level}
            onChange={(e) => setLevel(e.target.value === '' ? '' : Number(e.target.value))}
            className="w-full rounded-md border px-2 py-1"
            aria-label="Poziom zadania"
          >
            <option value="">Wybierz poziom</option>
            {LEVELS.map((lv) => <option key={lv} value={lv}>{lv}</option>)}
          </select>
        </FormItem>

        <FormItem className="mb-3">
          <Label className="mb-1">Autor (twoje id)</Label>
          <Input value={user?.id ?? ''} readOnly aria-label="Autor zadania" />
        </FormItem>

        {error && <div className="text-sm text-destructive mb-2">{error}</div>}

        <div className="flex justify-end gap-2 mt-4">
          <Button variant="outline" type="button" onClick={onClose}>Anuluj</Button>
          <Button type="submit" disabled={submitting}>{submitting ? 'Generowanie...' : 'Generuj'}</Button>
        </div>
      </form>
    </div>
  );
};

export default TaskGenerateDialog;
