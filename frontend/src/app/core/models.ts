export interface AuthResponse {
  accessToken: string;
  expiresIn: number;
  fullName: string;
  authorities: string[];
}

export interface ApiError { code: string; message: string; timestamp?: string; }
export type ApiRecord = Record<string, string | number | boolean | null>;
