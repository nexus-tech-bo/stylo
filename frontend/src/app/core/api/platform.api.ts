import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateBusinessRequest, PlatformBusiness } from '../models/api.models';

/** SUPERADMIN (platform token). */
@Injectable({ providedIn: 'root' })
export class PlatformApi {
  private readonly http = inject(HttpClient);

  businesses(): Observable<PlatformBusiness[]> {
    return this.http.get<PlatformBusiness[]>('platform/businesses');
  }

  createBusiness(request: CreateBusinessRequest): Observable<PlatformBusiness> {
    return this.http.post<PlatformBusiness>('platform/businesses', request);
  }

  setStatus(id: number, active: boolean): Observable<void> {
    return this.http.put<void>(`platform/businesses/${id}/status`, { active });
  }
}
