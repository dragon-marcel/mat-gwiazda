import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import type { AuthLoginCommand } from '../../types/api';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import {
  Form,
  FormField,
  FormItem,
  FormLabel,
  FormControl,
} from '../../components/ui/form';
import Stars from '../../components/ui/Stars';

const LoginForm: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  // store server errors as an array so we can render each on its own line
  const [globalError, setGlobalError] = useState<string[] | null>(null);

  const form = useForm<AuthLoginCommand>({
    defaultValues: { email: '', password: '' },
    mode: 'onBlur',
  });

  // keep showing client-side validation errors in the global banner whenever they appear
  React.useEffect(() => {
    const msgs = flattenFieldErrors(form.formState.errors);
    if (msgs.length) setGlobalError(msgs);
  }, [form.formState.errors]);

  const handleServerErrors = (errors: string[] | undefined) => {
    if (!errors || errors.length === 0) return false;
    // Keep server errors as an array so UI can render each on its own line
    setGlobalError(errors);
    return true;
  };

  const onSubmit = async (values: AuthLoginCommand) => {
    setGlobalError(null);
    form.clearErrors();
    try {
      await login(values);
      navigate('/play');
    } catch (err: any) {
      const serverErrors = err?.response?.data?.errors;
      const serverMessage = err?.response?.data?.message;
      if (Array.isArray(serverErrors) && serverErrors.length > 0) {
        handleServerErrors(serverErrors);
      } else if (serverMessage) {
        // display single message in global banner as array
        setGlobalError([String(serverMessage)]);
      } else {
        const msg = err?.message || 'Failed to login';
        setGlobalError([msg]);
      }
    }
  };

  // Collect client-side validation errors from react-hook-form into globalError
  const flattenFieldErrors = (errObj: any): string[] => {
    const out: string[] = [];
    const walk = (o: any) => {
      if (!o) return;
      if (Array.isArray(o)) {
        o.forEach(walk);
        return;
      }
      if (typeof o === 'object') {
        if (o.message) {
          out.push(String(o.message));
        }
        Object.values(o).forEach(walk);
      }
    };
    walk(errObj);
    return out;
  };

  const onInvalid = (errors: any) => {
    const msgs = flattenFieldErrors(errors);
    if (msgs.length) setGlobalError(msgs);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-slate-900">
      <div className="max-w-md w-full mx-auto p-6 bg-white dark:bg-slate-800 rounded-md shadow-md text-slate-900 dark:text-slate-100 relative">
        {/* Brand in top-right corner */}
        <div className="absolute top-3 right-3 flex items-center gap-2" aria-hidden="true">
          <Stars count={1} inline size="lg" />
          <span className="text-sm font-semibold text-slate-700 dark:text-slate-200">Mat-Gwiazda</span>
        </div>

        <h2 className="text-2xl font-semibold mb-4">Logowanie</h2>
        {globalError && (
          <ul className="mb-4 text-red-600 dark:text-red-400 text-sm space-y-1">
            {globalError.map((errMsg, i) => (
              <li key={i} className="leading-tight">- {errMsg}</li>
            ))}
          </ul>
        )}

        <Form {...form}>
          <form noValidate onSubmit={form.handleSubmit(onSubmit, onInvalid)}>
            <FormField
              control={form.control}
              name="email"
              rules={{
                required: 'Email jest wymagany',
                pattern: { value: /\S+@\S+\.\S+/, message: 'Nieprawidłowy format email' },
              }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="text-slate-700 dark:text-slate-200">Email</FormLabel>
                  <FormControl>
                    <Input {...field} type="email" />
                  </FormControl>
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="password"
              rules={{ required: 'Hasło jest wymagane', minLength: { value: 6, message: 'Hasło musi mieć co najmniej 6 znaków' } }}
              render={({ field }) => (
                <FormItem className="mt-4">
                  <FormLabel className="text-slate-700 dark:text-slate-200">Hasło</FormLabel>
                  <FormControl>
                    <Input {...field} type="password" />
                  </FormControl>
                </FormItem>
              )}
            />

            <div className="mt-6">
              <Button type="submit" disabled={form.formState.isSubmitting} className="bg-blue-600 text-white hover:bg-blue-700 focus:ring-2 focus:ring-blue-500" >
                {form.formState.isSubmitting ? 'Logowanie...' : 'Zaloguj się'}
              </Button>
            </div>
          </form>
        </Form>

        <div className="mt-4 text-sm">
          Nie masz konta? <Link to="/register" className="text-blue-600 dark:text-blue-400">Zarejestruj się</Link>
        </div>
      </div>
    </div>
  );
};

export default LoginForm;
