import { useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../api/apiClient';

type TaskDto = {
  id: string;
  level: number;
  prompt: string;
  options?: string[];
  correctOptionIndex?: number | null;
  explanation?: string | null;
  createdById?: string | null;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
};

type Page<T> = {
  content: T[];
  totalElements: number;
  number: number; // page index
  size: number;
};

// Fetch tasks (react-query)
const fetchTasks = async (params: Record<string, any>) => {
  const resp = await api.get('/tasks', { params });
  return resp.data as Page<TaskDto>;
};

const fetchTaskById = async (id: string) => {
  const resp = await api.get(`/tasks/${id}`);
  return resp.data as TaskDto;
};

const postGenerateTask = async (cmd: { level?: number; createdById?: string | null }) => {
  const resp = await api.post('/tasks/generate', cmd);
  return resp.data as TaskDto;
};

const patchToggleActive = async (id: string, isActive: boolean) => {
  const resp = await api.patch(`/tasks/${id}`, { isActive });
  return resp.data as TaskDto;
};

export const useTasksQuery = (params: Record<string, any>) => {
  // normalize params to stable object for query key
  // JSON stringify params into a stable string and use that as the hook dependency
  const paramsJson = JSON.stringify(params);
  // Use the serialized params as part of the key to avoid referencing the params object
  const key = useMemo(() => ['tasks', paramsJson], [paramsJson]);
  return useQuery<Page<TaskDto>, Error>({ queryKey: key, queryFn: () => fetchTasks(params) });
};

export const useTaskQuery = (id?: string | null) => {
  return useQuery<TaskDto, Error>({ queryKey: ['task', id], queryFn: () => fetchTaskById(id as string), enabled: !!id });
};

export const useGenerateTask = () => {
  const qc = useQueryClient();
  return useMutation<{ id: string; level?: number; createdById?: string | null }, Error, { level?: number; createdById?: string | null }>({
    mutationFn: (cmd: { level?: number; createdById?: string | null }) => postGenerateTask(cmd),
    onSuccess: (data) => {
      // Invalidate tasks list so it refetches
      qc.invalidateQueries({ queryKey: ['tasks'] });
      // Optionally, set a specific task cache
      qc.setQueryData({ queryKey: ['task', data.id] }, data);
    },
  });
};

export const useToggleActive = () => {
  const qc = useQueryClient();
  return useMutation<TaskDto, Error, { id: string; isActive: boolean }>({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) => patchToggleActive(id, isActive),
    onSuccess: (data) => {
      // update task cache and invalidate list
      qc.setQueryData({ queryKey: ['task', data.id] }, data);
      qc.invalidateQueries({ queryKey: ['tasks'] });
    },
  });
};

export type { TaskDto, Page };
