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
};

