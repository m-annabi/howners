export enum Role {
  OWNER = 'OWNER',
  TENANT = 'TENANT',
  CONCIERGE = 'CONCIERGE',
  ADMIN = 'ADMIN'
}

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: Role;
}
