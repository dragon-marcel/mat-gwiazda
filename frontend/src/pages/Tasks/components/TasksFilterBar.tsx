import React, { useEffect, useState } from 'react';
import { Label } from '../../../components/ui/label';
import { FormItem } from '../../../components/ui/form';

type Props = {
  onChange: (filters: Record<string, any>) => void;
};

const LEVELS = Array.from({ length: 8 }, (_, i) => i + 1);

const TasksFilterBar: React.FC<Props> = ({ onChange }) => {
  const [level, setLevel] = useState<number | ''>('');

  // notify on level change
  useEffect(() => {
    const t = setTimeout(() => {
      onChange({ level: level === '' ? undefined : level });
    }, 200);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [level]);

  return (
    <div className="flex flex-col md:flex-row md:items-end gap-3 p-3">
      <FormItem>
        <Label className="text-sm">Poziom</Label>
        <select
          value={level}
          onChange={(e) => setLevel(e.target.value === '' ? '' : Number(e.target.value))}
          className="rounded-md border px-2 py-1 text-black"
          aria-label="Filtr: poziom"
        >
          <option value="">Wszystkie</option>
          {LEVELS.map((lv) => (
            <option key={lv} value={lv} className="text-black">{lv}</option>
          ))}
        </select>
      </FormItem>
    </div>
  );
};

export default TasksFilterBar;
