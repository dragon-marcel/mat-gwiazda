import React from 'react';

export type StarSize = 'sm' | 'md' | 'lg';
export type StarVariant = 'yellow' | 'gold' | 'white' | 'primary';

export type StarsProps = {
  count?: number; // number of stars to render (clamped to 0..10)
  className?: string; // additional wrapper classes
  inline?: boolean; // render inline (small) star(s)
  size?: StarSize; // size of each star
  variant?: StarVariant; // color variant
};

const clamp = (n: number) => Math.max(0, Math.min(n, 10));

const sizeMap: Record<StarSize, string> = {
  sm: 'w-3 h-3',
  md: 'w-4 h-4',
  lg: 'w-6 h-6',
};

const variantMap: Record<StarVariant, string> = {
  yellow: 'text-yellow-400',
  gold: 'text-yellow-500',
  white: 'text-white',
  primary: 'text-primary',
};

export const Stars: React.FC<StarsProps> = ({ count = 0, className = '', inline = false, size = 'md', variant = 'yellow' }) => {
  const c = clamp(count);
  // Inject keyframes once via a style tag so the animation works wherever this component is used
  const keyframes = `@keyframes twinkle { 0%{opacity:0.35; transform:scale(0.9)} 50%{opacity:1; transform:scale(1.05)} 100%{opacity:0.35; transform:scale(0.9)} }`;

  const sizeClass = sizeMap[size] || sizeMap.md;
  const colorClass = variantMap[variant] || variantMap.yellow;

  // Inline single star: always render one small star (or sized by `size`)
  if (inline) {
    return (
      <>
        <style>{keyframes}</style>
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="currentColor"
          className={`inline-block ${sizeClass} ${colorClass} ml-2 drop-shadow-lg ${className}`}
          style={{ animationName: 'twinkle', animationDuration: '1200ms', animationIterationCount: 'infinite' }}
          aria-hidden
        >
          <path d="M12 .587l3.668 7.431 8.2 1.192-5.934 5.788 1.402 8.173L12 18.897l-7.336 3.874 1.402-8.173L.132 9.21l8.2-1.192z" />
        </svg>
      </>
    );
  }

  if (c === 0) return null;

  return (
    <>
      <style>{keyframes}</style>
      <div className={`flex items-center gap-1 pointer-events-none ${className}`}>
        {Array.from({ length: c }).map((_, i) => (
          <svg
            key={i}
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="currentColor"
            className={`${sizeClass} ${colorClass} drop-shadow-lg`}
            style={{ animationName: 'twinkle', animationDelay: `${i * 120}ms`, animationDuration: '1200ms', animationIterationCount: 'infinite' }}
          >
            <path d="M12 .587l3.668 7.431 8.2 1.192-5.934 5.788 1.402 8.173L12 18.897l-7.336 3.874 1.402-8.173L.132 9.21l8.2-1.192z" />
          </svg>
        ))}
      </div>
    </>
  );
};

export default Stars;
