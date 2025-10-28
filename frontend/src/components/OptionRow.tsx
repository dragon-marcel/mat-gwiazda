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
  revealCorrect?: boolean;
};

// Simple helper: options only change when selected (blue). They do NOT show correctness colors here.
function computeContainerClass(i: number, selectedOption: number | null, revealCorrect?: boolean, correctIndex?: number | null) {
  const base = 'p-2 rounded-md transition-colors';
  if (revealCorrect) {
    // Reveal mode: no gray border, correct option highlighted green, selected (but not correct) gets subtle bg
    if (typeof correctIndex === 'number' && correctIndex === i) return `${base} bg-green-600 text-white font-semibold`;
    if (selectedOption === i) return `${base} bg-white/5 text-white`;
    return `${base} text-white/90`;
  }

  // Default Play mode: keep border and blue selected state
  const defaultClass = `${base} border transition-colors border-slate-200 dark:border-slate-700 bg-transparent`;
  if (selectedOption === i) return `${base} bg-sky-600 border-sky-600 text-white shadow-sm`;
  return defaultClass;
}

const OptionRow: React.FC<Props> = ({ opt, i, selectedOption, result, correctIndex, disabled, onSelect, revealCorrect = false }) => {
  // Note: By default OptionRow does not reveal correctness. If `revealCorrect` is true
  // the row will highlight the correct option instead (used by TaskDetailsModal).
  const containerClass = computeContainerClass(i, selectedOption, revealCorrect, correctIndex);
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
        <span className="text-sm">{opt}</span>
      </label>
    </div>
  );
};

export default React.memo(OptionRow);
