import React, { useEffect, useState, useCallback, useRef } from 'react';
import PageLayout from '../../components/PageLayout';
import { getAllUsers, getLearningLevels, createLearningLevel, updateLearningLevel, deleteLearningLevel } from '../../lib/services/adminService';
import type { UserDto, LearningLevelDto, CreateLearningLevelCommand, UpdateLearningLevelCommand } from '../../types/api';
import ErrorBanner from '../../components/ui/ErrorBanner';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../contexts/AuthContext';

const AdminUsersView: React.FC = () => {
  const { user } = useAuth();
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | undefined>(undefined);

  const [activeTab, setActiveTab] = useState<'users' | 'levels'>('users');
  const isAdmin = user?.role === 'ADMIN';

  // --- Users ---
  const loadUsers = useCallback(async () => {
    setError(undefined);
    setLoading(true);
    try {
      const data = await getAllUsers();
      setUsers(data);
    } catch (e: any) {
      const msg = e?.response?.data?.message ?? e?.message ?? 'Błąd ładowania użytkowników';
      setError(String(msg));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isAdmin) {
      setUsers([]);
      setLoading(false);
      return;
    }
    void loadUsers();
  }, [isAdmin, loadUsers]);

  // --- Learning levels ---
  const [levels, setLevels] = useState<LearningLevelDto[]>([]);
  const [levelsLoading, setLevelsLoading] = useState(false);
  const [levelsError, setLevelsError] = useState<string | undefined>(undefined);

  const [formMode, setFormMode] = useState<'create' | 'edit'>('create');
  const [formLevel, setFormLevel] = useState<number | ''>('');
  const [formTitle, setFormTitle] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [editingLevel, setEditingLevel] = useState<number | null>(null);
  const [confirmDeleteLevel, setConfirmDeleteLevel] = useState<number | null>(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  const loadLevels = useCallback(async () => {
    setLevelsError(undefined);
    setLevelsLoading(true);
    try {
      const data = await getLearningLevels();
      setLevels(data.sort((a, b) => a.level - b.level));
    } catch (e: any) {
      const msg = e?.response?.data?.message ?? e?.message ?? 'Błąd ładowania poziomów';
      setLevelsError(String(msg));
    } finally {
      setLevelsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isAdmin) return;
    if (activeTab === 'levels') void loadLevels();
  }, [isAdmin, activeTab, loadLevels]);

  const resetForm = () => {
    setFormMode('create');
    setFormLevel('');
    setFormTitle('');
    setFormDescription('');
    setEditingLevel(null);
    setLevelsError(undefined);
  };

  const handleEdit = (lvl: LearningLevelDto) => {
    setFormMode('edit');
    setEditingLevel(lvl.level);
    setFormLevel(lvl.level);
    setFormTitle(lvl.title);
    setFormDescription(lvl.description);
    setLevelsError(undefined);
  };

  // open modal to confirm deletion
  const handleDelete = (level: number) => {
    setConfirmDeleteLevel(level);
  };

  const confirmBtnRef = useRef<HTMLButtonElement | null>(null);

  const performDeleteConfirmed = async () => {
    if (confirmDeleteLevel === null) return;
    setDeleteLoading(true);
    setLevelsError(undefined);
    try {
      await deleteLearningLevel(confirmDeleteLevel);
      setConfirmDeleteLevel(null);
      await loadLevels();
    } catch (e: any) {
      const msg = e?.response?.data?.message ?? e?.message ?? 'Błąd usuwania poziomu';
      setLevelsError(String(msg));
    } finally {
      setDeleteLoading(false);
    }
  };

  // Manage focus and keyboard while modal is open
  useEffect(() => {
    if (confirmDeleteLevel !== null) {
      // focus confirm button when modal opens
      setTimeout(() => confirmBtnRef.current?.focus(), 0);
      // prevent background scroll
      const prev = document.body.style.overflow;
      document.body.style.overflow = 'hidden';

      const onKey = (ev: KeyboardEvent) => {
        if (ev.key === 'Escape') {
          setConfirmDeleteLevel(null);
        }
      };
      window.addEventListener('keydown', onKey);
      return () => {
        window.removeEventListener('keydown', onKey);
        document.body.style.overflow = prev;
      };
    }
    return undefined;
  }, [confirmDeleteLevel]);

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    setLevelsError(undefined);

    // validation
    if (formMode === 'create' && (formLevel === '' || Number.isNaN(Number(formLevel)))) {
      setLevelsError('Podaj poprawny numer poziomu.');
      return;
    }
    if (!formTitle.trim()) {
      setLevelsError('Podaj tytuł poziomu.');
      return;
    }
    if (!formDescription.trim()) {
      setLevelsError('Podaj opis poziomu.');
      return;
    }

    try {
      if (formMode === 'create') {
        const cmd: CreateLearningLevelCommand = {
          level: Number(formLevel),
          title: formTitle.trim(),
          description: formDescription.trim(),
        };
        await createLearningLevel(cmd);
      } else if (formMode === 'edit' && editingLevel !== null) {
        const cmd: UpdateLearningLevelCommand = {
          title: formTitle.trim(),
          description: formDescription.trim(),
        };
        await updateLearningLevel(editingLevel, cmd);
      }
      resetForm();
      await loadLevels();
    } catch (e: any) {
      const msg = e?.response?.data?.message ?? e?.message ?? 'Błąd zapisu poziomu';
      setLevelsError(String(msg));
    }
  };

  if (!isAdmin) {
    return (
      <PageLayout title="Panel administracyjny">
        <div className="p-4">
          <ErrorBanner message={user ? 'Brak uprawnień — dostęp zabroniony.' : 'Zaloguj się, aby uzyskać dostęp do panelu administracyjnego.'} />
        </div>
      </PageLayout>
    );
  }

  // users tbody
  let usersTbody: React.ReactNode;
  if (loading) {
    usersTbody = (
      <tr>
        <td colSpan={7} className="py-6 text-center text-sm text-slate-500">Ładowanie użytkowników…</td>
      </tr>
    );
  } else if (users.length === 0) {
    usersTbody = (
      <tr>
        <td colSpan={7} className="py-6 text-center text-sm text-slate-500">Brak użytkowników do wyświetlenia.</td>
      </tr>
    );
  } else {
    usersTbody = users.map((u) => {
      const activeFlag = typeof u.active === 'boolean' ? u.active : !!u.isActive;

      return (
        <tr key={u.id} className="border-b last:border-b-0">
          <td className="py-2 pr-4">{u.userName ?? '—'}</td>
          <td className="py-2 pr-4 text-sm text-slate-600 dark:text-slate-300">{u.email}</td>
          <td className="py-2 pr-4">{u.role ?? '—'}</td>
          <td className="py-2 pr-4">{u.currentLevel ?? '—'}</td>
          <td className="py-2 pr-4">{u.points ?? 0}</td>
          <td className="py-2 pr-4">{u.stars ?? 0}</td>
          <td className="py-2 pr-4">{activeFlag ? 'Tak' : 'Nie'}</td>
        </tr>
      );
    });
  }

  return (
    <PageLayout title="Panel administracyjny">
      <main>
        <section className="mb-4">
          <div className="flex items-center">
            <div role="tablist" aria-label="Panel zakładek" className="inline-flex items-center gap-2 bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-md p-1">
              <Button
                role="tab"
                aria-selected={activeTab === 'users'}
                variant="ghost"
                size="sm"
                onClick={() => setActiveTab('users')}
                className={activeTab === 'users' ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm ring-1 ring-indigo-600' : 'text-slate-700 dark:text-slate-200'}
              >
                Użytkownicy
              </Button>

              <Button
                role="tab"
                aria-selected={activeTab === 'levels'}
                variant="ghost"
                size="sm"
                onClick={() => setActiveTab('levels')}
                className={activeTab === 'levels' ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm ring-1 ring-indigo-600' : 'text-slate-700 dark:text-slate-200'}
              >
                Poziomy
              </Button>
            </div>
          </div>
        </section>

        <section>
          <ErrorBanner message={error} />

          <div className="p-4 border rounded-md bg-white dark:bg-slate-800 shadow-sm">
            {activeTab === 'users' ? (
              <div className="overflow-x-auto">
                <table className="w-full table-auto border-collapse">
                  <thead>
                    <tr className="text-left border-b">
                      <th className="py-2 pr-4">Nazwa</th>
                      <th className="py-2 pr-4">Email</th>
                      <th className="py-2 pr-4">Rola</th>
                      <th className="py-2 pr-4">Poziom</th>
                      <th className="py-2 pr-4">Punkty</th>
                      <th className="py-2 pr-4">Gwiazdki</th>
                      <th className="py-2 pr-4">Aktywny</th>
                    </tr>
                  </thead>
                  <tbody>{usersTbody}</tbody>
                </table>
              </div>
            ) : (
              <div className="space-y-4">
                <ErrorBanner message={levelsError} />

                <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-2 md:grid-cols-6 md:items-end">
                  <div className="md:col-span-1">
                    <label htmlFor="level-input" className="block text-sm font-medium mb-1">Poziom</label>
                    <input
                      id="level-input"
                      name="level"
                      type="number"
                      min={1}
                      value={formLevel}
                      onChange={(e) => setFormLevel(e.target.value === '' ? '' : Number(e.target.value))}
                      className="w-full p-2 border rounded-md bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                      disabled={formMode === 'edit'}
                      aria-label="numer poziomu"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label htmlFor="title-input" className="block text-sm font-medium mb-1">Tytuł</label>
                    <input
                      id="title-input"
                      name="title"
                      type="text"
                      value={formTitle}
                      onChange={(e) => setFormTitle(e.target.value)}
                      className="w-full p-2 border rounded-md bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                      aria-label="tytuł poziomu"
                    />
                  </div>

                  <div className="md:col-span-3 md:col-span-6">
                    <label htmlFor="description-input" className="block text-sm font-medium mb-1">Opis</label>
                    <textarea
                      id="description-input"
                      name="description"
                      rows={4}
                      value={formDescription}
                      onChange={(e) => setFormDescription(e.target.value)}
                      className="w-full p-2 border rounded-md bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                      aria-label="opis poziomu"
                    />
                  </div>

                  <div className="md:col-span-6 flex gap-2">
                    <Button type="submit" size="sm" variant="default" className="bg-indigo-600 hover:bg-indigo-700 text-white">{formMode === 'create' ? 'Dodaj poziom' : 'Zapisz zmiany'}</Button>
                    <Button type="button" size="sm" variant="default" className="bg-indigo-600 hover:bg-indigo-700 text-white" onClick={resetForm}>Anuluj</Button>
                  </div>
                </form>

                <div className="overflow-x-auto">
                  <table className="w-full table-auto border-collapse">
                    <thead>
                      <tr className="text-left border-b">
                        <th className="py-2 pr-4">Poziom</th>
                        <th className="py-2 pr-4">Tytuł</th>
                        <th className="py-2 pr-4">Opis</th>
                        <th className="py-2 pr-4">Akcje</th>
                      </tr>
                    </thead>
                    <tbody>
                      {levelsLoading ? (
                        <tr>
                          <td colSpan={4} className="py-6 text-center text-sm text-slate-500">Ładowanie poziomów…</td>
                        </tr>
                      ) : levels.length === 0 ? (
                        <tr>
                          <td colSpan={4} className="py-6 text-center text-sm text-slate-500">Brak poziomów do wyświetlenia.</td>
                        </tr>
                      ) : (
                        levels.map((lvl) => (
                          <tr key={lvl.level} className="border-b last:border-b-0">
                            <td className="py-2 pr-4">{lvl.level}</td>
                            <td className="py-2 pr-4">{lvl.title}</td>
                            <td className="py-2 pr-4 break-words max-w-[40ch]">{lvl.description}</td>
                            <td className="py-2 pr-4">
                              <div className="flex gap-2">
                                <Button size="sm" variant="default" className="bg-indigo-600 hover:bg-indigo-700 text-white" onClick={() => handleEdit(lvl)}>Edytuj</Button>
                                <Button size="sm" variant="default" className="bg-indigo-600 hover:bg-indigo-700 text-white" onClick={() => handleDelete(lvl.level)}>Usuń</Button>
                              </div>
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </section>

        {/* Delete confirmation modal */}
        {confirmDeleteLevel !== null && (
          <div className="fixed inset-0 z-50 flex items-center justify-center" onClick={() => setConfirmDeleteLevel(null)}>
            <div className="absolute inset-0 bg-black/40" aria-hidden="true" />
            <div role="dialog" aria-modal="true" aria-labelledby="delete-title" className="relative bg-white dark:bg-slate-900 rounded-lg p-6 w-[90%] max-w-md mx-4 text-center shadow-lg" onClick={(e) => e.stopPropagation()}>
              <h3 id="delete-title" className="text-lg font-semibold mb-2">Potwierdź usunięcie</h3>
              <p className="mb-4">Czy na pewno chcesz usunąć poziom <strong>{confirmDeleteLevel}</strong>?</p>
              {levelsError && <ErrorBanner message={levelsError} />}
              <div className="flex justify-center gap-2 mt-4">
                <Button size="sm" variant="ghost" onClick={() => setConfirmDeleteLevel(null)}>Anuluj</Button>
                <Button ref={confirmBtnRef as any} size="sm" variant="destructive" className="bg-red-600 hover:bg-red-700 text-white" onClick={performDeleteConfirmed} disabled={deleteLoading}>
                  {deleteLoading ? 'Usuwanie...' : 'Usuń poziom'}
                </Button>
              </div>
            </div>
          </div>
        )}
      </main>
    </PageLayout>
  );
};

export default AdminUsersView;

// NOTE: removed old prompt UI block to prevent accessibility/key warnings
