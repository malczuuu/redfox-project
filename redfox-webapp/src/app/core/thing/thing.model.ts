export interface Thing {
  id: string;
  code: string;
  name: string;
  description: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface CreateThing {
  code: string;
  name: string;
  description: string;
}

export interface UpdateThing {
  name: string;
  description: string;
  version: number;
}
