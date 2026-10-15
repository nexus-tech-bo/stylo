import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BusinessRequest, BusinessResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class BusinessApi {
  private readonly http = inject(HttpClient);

  /** Any business role. */
  get(): Observable<BusinessResponse> {
    return this.http.get<BusinessResponse>('business');
  }

  /** ADMIN. */
  update(request: BusinessRequest): Observable<BusinessResponse> {
    return this.http.put<BusinessResponse>('business', request);
  }
}
