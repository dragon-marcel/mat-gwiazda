import React from 'react';
import type { ProgressSubmitResponseDto } from '../types/api';

type Props = {
  opt: string;
  i: number;
  selectedOption: number | null;
  result: ProgressSubmitResponseDto | null;
  correctIndex: number | null;
  disabled?: boolean;
  onSelect: (index: number) => void;
};

// Simple helper: options only change when selected (blue). They do NOT show correctness colors here.
function computeContainerClass(i: number, selectedOption: number | null) {
  const base = 'p-2 rounded-md border transition-colors';
  const defaultClass = `${base} border-slate-200 dark:border-slate-700 bg-transparent`;
  if (selectedOption === i) return `${base} bg-sky-600 border-sky-600 text-white shadow-sm`;
  return defaultClass;
}

const OptionRow: React.FC<Props> = ({ opt, i, selectedOption, result, correctIndex, disabled, onSelect }) => {
  // Note: we intentionally ignore `result` and `correctIndex` here so individual options
  // don't change color after submit. Coloring of correctness is handled centrally on the Next button.
  const containerClass = computeContainerClass(i, selectedOption);
  const inputId = `option-${i}`;

  return (
    <div className={containerClass}>
      <input
        id={inputId}
        type="radio"
        name={`task-option`}
        checked={selectedOption === i}
        onChange={() => onSelect(i)}
        className="sr-only"
        disabled={disabled}
        aria-label={opt}
      />
      <label htmlFor={inputId} className="flex items-center cursor-pointer" aria-label={opt}>
        <span className={`text-sm ${containerClass.includes('text-white') ? 'text-white' : 'text-slate-800 dark:text-slate-100'}`}>{opt}</span>
      </label>
    </div>
  );
};

export default React.memo(OptionRow);
