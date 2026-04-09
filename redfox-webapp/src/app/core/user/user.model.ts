export interface User {
  id: string;
  login: string;
  firstName: string;
  lastName: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateUser {
  login: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UpdateUser {
  firstName: string;
  lastName: string;
  version: number;
}
