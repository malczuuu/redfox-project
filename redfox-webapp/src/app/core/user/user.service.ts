import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Identity, PageResult } from '../common/common.model';
import { CreateUser, UpdateUser, User } from './user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly baseUrl = '/api/v1/users';

  constructor(private readonly http: HttpClient) {}

  getUsers(page = 0, size = 20): Observable<PageResult<User>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResult<User>>(this.baseUrl, { params });
  }

  getUser(id: string): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  createUser(request: CreateUser): Observable<Identity> {
    return this.http.post<Identity>(this.baseUrl, request);
  }

  updateUser(id: string, request: UpdateUser): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, request);
  }

  deleteUser(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
