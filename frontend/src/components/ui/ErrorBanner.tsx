import React from 'react';

const ErrorBanner: React.FC<{ message?: string }> = ({ message }) => {
  if (!message) return null;
  return <div className="bg-red-50 border border-red-200 text-red-700 px-3 py-2 rounded mb-4">{message}</div>;
};

export default ErrorBanner;
