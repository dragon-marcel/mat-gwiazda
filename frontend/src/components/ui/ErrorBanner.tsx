import React from 'react';

const ErrorBanner: React.FC<{ message?: string }> = ({ message }) => {
  if (!message) return null;
  return (
    <div role="alert" aria-live="assertive" className="bg-red-700 text-white px-3 py-2 rounded mb-4">
      {message}
    </div>
  );
};

export default ErrorBanner;
