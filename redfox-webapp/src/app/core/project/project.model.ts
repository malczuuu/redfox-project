export interface Project {
  id: string;
  code: string;
  name: string;
  description: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateProject {
  code: string;
  name: string;
  description: string;
}

export interface UpdateProject {
  name: string;
  description: string;
  version: number;
}
