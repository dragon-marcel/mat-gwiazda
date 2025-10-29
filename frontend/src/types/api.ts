export type AuthLoginCommand = {
  email: string;
  password: string;
};

export type AuthRegisterCommand = {
  email: string;
  password: string;
  userName: string;
};

export type AuthResponseDto = {
  accessToken: string;
  expiresIn?: number;
  refreshToken?: string;
};

export type UserDto = {
  id: string;
  email: string;
  userName?: string;
  currentLevel?: number;
  points?: number;
  stars?: number;
  role?: string; // optional role (e.g. 'ADMIN', 'STUDENT') provided by backend
  active?: boolean; // maps to backend UserDto.isActive -> serialized as 'active'
  isActive?: boolean; // fallback if backend provides the boolean under this name
};

// Task / Play related types used by frontend
export type TaskDto = {
  id: string;
  // text shown to the user
  prompt: string;
  // multiple-choice options (if present)
  options?: string[];
  // authoritative correct index when backend exposes it (may be omitted)
  correctOptionIndex?: number | null;
  // optional explanation text
  explanation?: string | null;
  // level/difficulty used by generator
  level?: number;
};

export type TaskListDto = TaskDto[];

export type AnswerCommand = {
  taskId: string;
  answer: string | number;
  timeTakenMs?: number;
};

export type AnswerResultDto = {
  correct: boolean;
  gainedPoints: number;
  newLevel?: number;
  newPoints?: number;
  message?: string; // optional human-friendly message
};

// Progress-related types mirror backend DTOs (ProgressSubmitCommand / ProgressSubmitResponseDto)
export type ProgressSubmitCommand = {
  // Progress submit should reference the server-side Progress entity created when generating a task
  progressId: string;
  selectedOptionIndex: number;
  timeTakenMs?: number;
};

export type ProgressSubmitResponseDto = {
  progressId: string;
  isCorrect: boolean;
  pointsAwarded: number;
  userPoints: number;
  starsAwarded: number;
  leveledUp: boolean;
  newLevel?: number;
  explanation?: string | null;
};

// Response for generate task endpoint which also returns a progress id
export type TaskWithProgressDto = {
  task: TaskDto;
  progressId: string;
};

// Learning levels types — mirror backend migration and DTOs
export type LearningLevelDto = {
  level: number; // smallint primary key
  title: string;
  description: string;
  createdBy?: string | null;
  createdAt?: string | null; // ISO timestamp
  modifiedBy?: string | null;
  modifiedAt?: string | null; // ISO timestamp
};

export type CreateLearningLevelCommand = {
  level: number;
  title: string;
  description: string;
};

export type UpdateLearningLevelCommand = {
  title?: string;
  description?: string;
};
