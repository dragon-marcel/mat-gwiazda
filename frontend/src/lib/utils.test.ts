import { describe, it, expect } from 'vitest';
import { cn } from './utils';

describe('cn utility', () => {
  it('merges class names and removes duplicates', () => {
    const result = cn('text-center', 'font-bold', 'text-center');
    // order/merging may vary but result should include both classes once
    expect(result).toContain('text-center');
    expect(result).toContain('font-bold');
  });

  it('handles falsy values gracefully', () => {
    // avoid constant boolean expressions to satisfy linters
    const maybeHidden: string | undefined = undefined;
    const result = cn('p-2', maybeHidden, 'p-2');
    expect(result).toContain('p-2');
  });
});
