import api from '../../api/apiClient';
import type { UserDto } from '../../types/api';

export async function getMe(): Promise<UserDto> {
  const resp = await api.get<UserDto>('/users/me');
  return resp.data;
}

export async function updateMe(payload: any): Promise<UserDto> {
  const resp = await api.patch<UserDto>('/users/me', payload);
  return resp.data;
}

export async function deleteMe(): Promise<void> {
  await api.delete('/users/me');
}
