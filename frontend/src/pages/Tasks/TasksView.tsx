import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useTasksQuery, useTaskQuery } from '../../hooks/useTasks';
import TasksFilterBar from './components/TasksFilterBar';
import TasksTable from './components/TasksTable';
import TaskDetailsModal from './components/TaskDetailsModal';
import { Button } from '../../components/ui/Button';
import PageLayout from '../../components/PageLayout';

const DEFAULT_PAGE_SIZE = 10;

const TasksView: React.FC = () => {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(DEFAULT_PAGE_SIZE);
  const [filters, setFilters] = useState<Record<string, any>>({});
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);

  const params = useMemo(() => ({ page, size, ...filters, ...(user?.id ? { createdById: user.id } : {}) }), [page, size, filters, user?.id]);

  const { data, isLoading, error } = useTasksQuery(params);
  const taskQuery = useTaskQuery(selectedTaskId);

  const handleFilterChange = (next: Record<string, any>) => {
    setPage(0);
    setFilters((prev) => ({ ...prev, ...next }));
  };

  const handleOpenTask = (id: string) => {
    setSelectedTaskId(id);
  };

  const handleCloseDetails = () => setSelectedTaskId(null);

  return (
    <PageLayout title="Przegląd zadań">
      <main className="relative bg-white dark:bg-slate-800 rounded-md shadow-md p-6 overflow-hidden">
        <section className="mb-4">
          <div className="flex items-center justify-between">
            <div />
            <div />
          </div>
        </section>

        <section>
          <TasksFilterBar onChange={handleFilterChange} />

          <div className="bg-white dark:bg-slate-800 rounded-md shadow-sm">
            <TasksTable
              page={data ?? null}
              loading={isLoading}
              onOpenTask={handleOpenTask}
            />
          </div>

          <div className="flex items-center justify-between mt-2">
            <div className="text-sm text-muted-foreground">
              {data ? `Strona ${data.number + 1} z ${Math.max(1, Math.ceil(data.totalElements / data.size))}` : ''}
            </div>
            <div className="flex gap-2">
              <Button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>Poprzednia</Button>
              <Button onClick={() => setPage((p) => p + 1)}>Następna</Button>
            </div>
          </div>

          {selectedTaskId && taskQuery.data && (
            <TaskDetailsModal task={taskQuery.data} onClose={handleCloseDetails} />
          )}

          {error && (
            <div className="p-2 text-sm text-destructive">Błąd: {(error as any)?.message ?? 'Nieznany błąd'}</div>
          )}
        </section>
      </main>
    </PageLayout>
  );
};

export default TasksView;
