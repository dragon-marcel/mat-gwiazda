// filepath: c:\Users\drago\Desktop\szkolenie AI\mat-gwiazda\frontend\src\lib\services\playService.ts
import api from '../../api/apiClient';
import type { TaskListDto, TaskDto, ProgressSubmitCommand, ProgressSubmitResponseDto } from '../../types/api';

/**
 * Service for interacting with Play/Task endpoints.
 */
export const listTasks = async (params?: { level?: number; page?: number; size?: number }) => {
  const query: Record<string, any> = {};
  if (params?.level !== undefined) query.level = params.level;
  if (params?.page !== undefined) query.page = params.page;
  if (params?.size !== undefined) query.size = params.size;
  const resp = await api.get('/tasks', { params: query });
  // Backend returns Spring Page<TaskDto> for list endpoint. If so, return content array; otherwise assume array.
  const data = resp.data as any;
  if (data && Array.isArray(data.content)) return data.content as TaskListDto;
  if (Array.isArray(data)) return data as TaskListDto;
  // Fallback: empty array
  return [] as TaskListDto;
};

export const getTask = async (taskId: string) => {
  const resp = await api.get<TaskDto>(`/tasks/${taskId}`);
  return resp.data;
};

export const generateTask = async (level: number, createdById?: string) => {
  const cmd: any = { level };
  if (createdById) cmd.createdById = createdById;
  const resp = await api.post<TaskDto>('/tasks/generate', cmd);
  return resp.data;
};

/**
 * Submit progress for a task attempt using ProgressController endpoint.
 * X-User-Id is attached automatically by apiClient interceptor from localStorage.
 */
export const submitProgress = async (userId: string, cmd: ProgressSubmitCommand): Promise<ProgressSubmitResponseDto> => {
  try {
    // we still pass userId here for clarity, but apiClient adds X-User-Id automatically
    const resp = await api.post<ProgressSubmitResponseDto>('/progress/submit', cmd, {
      headers: {
        // Keep explicit header to be safe in environments where interceptor might not run
        'X-User-Id': userId,
      },
    });
    return resp.data;
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('submitProgress failed', e);
    throw new Error((e as any)?.response?.data?.message ?? (e as Error)?.message ?? 'Unknown error during submitProgress');
  }
};
