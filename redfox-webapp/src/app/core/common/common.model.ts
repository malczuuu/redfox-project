export interface AccessToken {
  accessToken: string;
  expiresIn: number;
}

export interface Identity {
  id: string;
}

export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}
