import api from '../../api/apiClient';
import type { UserDto, LearningLevelDto, CreateLearningLevelCommand, UpdateLearningLevelCommand } from '../../types/api';

/**
 * Simple admin service wrapping backend endpoints used by the admin UI.
 * - GET /admin/users -> List<UserDto>
 * - PATCH /admin/users/{id} -> partial update (we use { active })
 * - Learning levels endpoints: /admin/learning-levels
 * Fallbacks: if admin endpoint isn't available, try /users/{id} for user updates.
 */

export const getAllUsers = async (): Promise<UserDto[]> => {
  const resp = await api.get<UserDto[]>('/admin/users');
  return resp.data;
};

export const updateUserActive = async (userId: string, active: boolean): Promise<UserDto> => {
  // try admin endpoint first
  try {
    const resp = await api.patch<UserDto>(`/admin/users/${userId}`, { active });
    return resp.data;
  } catch (e: any) {
    // If 404 or not found, try non-admin user endpoint as fallback
    const status = e?.response?.status;
    if (status === 404 || status === 405) {
      // let the fallback request throw if it fails - propagate that error to caller
      const resp2 = await api.patch<UserDto>(`/users/${userId}`, { active });
      return resp2.data;
    }
    throw e;
  }
};

// Learning levels CRUD
export const getLearningLevels = async (): Promise<LearningLevelDto[]> => {
  const resp = await api.get<LearningLevelDto[]>('/admin/learning-levels');
  return resp.data;
};

export const createLearningLevel = async (cmd: CreateLearningLevelCommand): Promise<LearningLevelDto> => {
  const resp = await api.post<LearningLevelDto>('/admin/learning-levels', cmd);
  return resp.data;
};

export const updateLearningLevel = async (level: number, cmd: UpdateLearningLevelCommand): Promise<LearningLevelDto> => {
  const resp = await api.put<LearningLevelDto>(`/admin/learning-levels/${level}`, cmd);
  return resp.data;
};

export const deleteLearningLevel = async (level: number): Promise<void> => {
  await api.delete(`/admin/learning-levels/${level}`);
};

export default {
  getAllUsers,
  updateUserActive,
  getLearningLevels,
  createLearningLevel,
  updateLearningLevel,
  deleteLearningLevel,
};
