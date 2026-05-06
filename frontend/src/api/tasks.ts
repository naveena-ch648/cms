import apiClient from './client';
import type { TaskItem, CreateTaskRequest, UpdateTaskRequest } from '../types/collaboration';

interface ApiResponse<T> {
  success: boolean;
  data: T;
}

export const tasksApi = {
  getFileTasks: (fileId: string, page = 0, size = 50) =>
    apiClient.get<ApiResponse<TaskItem[]>>(`/files/${fileId}/tasks`, { params: { page, size } }),

  createTask: (fileId: string, request: CreateTaskRequest) =>
    apiClient.post<ApiResponse<TaskItem>>(`/files/${fileId}/tasks`, request),

  updateTask: (taskId: string, request: UpdateTaskRequest) =>
    apiClient.patch<ApiResponse<TaskItem>>(`/tasks/${taskId}`, request),

  deleteTask: (taskId: string) =>
    apiClient.delete(`/tasks/${taskId}`),

  getMyTasks: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<TaskItem[]>>(`/tasks/my`, { params: { page, size } }),
};
