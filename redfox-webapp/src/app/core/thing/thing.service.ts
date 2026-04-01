import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Identity, PageResult } from '../common/common.model';
import { CreateThing, Thing, UpdateThing } from './thing.model';

@Injectable({ providedIn: 'root' })
export class ThingService {
  private readonly baseUrl = '/api/v1/projects';

  constructor(private readonly http: HttpClient) {}

  getThings(projectId: string, page = 0, size = 20): Observable<PageResult<Thing>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResult<Thing>>(`${this.baseUrl}/${projectId}/things`, { params });
  }

  getThing(projectId: string, id: string): Observable<Thing> {
    return this.http.get<Thing>(`${this.baseUrl}/${projectId}/things/${id}`);
  }

  createThing(projectId: string, request: CreateThing): Observable<Identity> {
    return this.http.post<Identity>(`${this.baseUrl}/${projectId}/things`, request);
  }

  updateThing(projectId: string, id: string, request: UpdateThing): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${projectId}/things/${id}`, request);
  }

  deleteThing(projectId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/things/${id}`);
  }
}
