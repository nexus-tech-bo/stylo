import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { StylistOption, UserRequest, UserResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class UsersApi {
  private readonly http = inject(HttpClient);

  /** ADMIN. */
  list(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>('users');
  }

  /** Any business role; active users with role STYLIST. */
  stylists(): Observable<StylistOption[]> {
    return this.http.get<StylistOption[]>('users/stylists');
  }

  /** ADMIN; password required. */
  create(request: UserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>('users', request);
  }

  /** ADMIN; password omitted = unchanged. */
  update(id: number, request: UserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`users/${id}`, request);
  }

  /** Any business role. */
  changeMyPassword(password: string): Observable<void> {
    return this.http.put<void>('users/me/password', { password });
  }
}
