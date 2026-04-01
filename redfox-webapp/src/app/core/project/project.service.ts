import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Identity, PageResult } from '../common/common.model';
import { CreateProject, Project, UpdateProject } from './project.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private readonly baseUrl = '/api/v1/projects';

  constructor(private readonly http: HttpClient) {}

  getProjects(page = 0, size = 20): Observable<PageResult<Project>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResult<Project>>(this.baseUrl, { params });
  }

  getProject(id: string): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/${id}`);
  }

  createProject(request: CreateProject): Observable<Identity> {
    return this.http.post<Identity>(this.baseUrl, request);
  }

  updateProject(id: string, request: UpdateProject): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${id}`, request);
  }

  deleteProject(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
