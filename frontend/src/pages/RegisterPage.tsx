import React from 'react';
import { Button } from '../components/ui/Button';
import Stars from '../components/ui/Stars';

const RegisterPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-slate-900 p-6">
      <div className="w-full max-w-md bg-white dark:bg-slate-800 rounded-md shadow-md p-6">
        <header className="mb-6 flex items-center justify-between">
          <h2 className="text-lg font-semibold">Zarejestruj się w <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Mat-Gwiazda</span><Stars count={1} inline /></h2>
        </header>

        <form className="space-y-4">
          <div>
            <label className="block text-sm mb-1">Nazwa użytkownika</label>
            <input className="w-full rounded border px-3 py-2" type="text" />
          </div>
          <div>
            <label className="block text-sm mb-1">Email</label>
            <input className="w-full rounded border px-3 py-2" type="email" />
          </div>
          <div>
            <label className="block text-sm mb-1">Hasło</label>
            <input className="w-full rounded border px-3 py-2" type="password" />
          </div>
          <div className="flex items-center justify-between">
            <Button size="lg">Zarejestruj się</Button>
            <Button variant="link" size="sm">Masz już konto? Zaloguj</Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RegisterPage;

