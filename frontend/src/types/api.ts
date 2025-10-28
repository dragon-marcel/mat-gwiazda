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
};

// Task / Play related types used by frontend
export type TaskDto = {
  id: string;
  title: string;
  description?: string;
  question: string; // question text (could include simple markup)
  options?: string[]; // optional multiple-choice options
  // if options is present, correctOptionIndex may be set on backend (frontend won't rely on it)
  difficulty?: 'easy' | 'medium' | 'hard';
  points: number; // points awarded for correct answer
  timeLimitSeconds?: number; // optional time limit for the task
  hints?: string[];
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
  taskId: string;
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
