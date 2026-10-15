import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  LoginRequest,
  LoginResponse,
  PlatformLoginRequest,
  PlatformLoginResponse,
  UserResponse,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly http = inject(HttpClient);

  /** Public. */
  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('auth/login', request);
  }

  /** Any business role. */
  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>('auth/me');
  }

  /** Public (platform). */
  platformLogin(request: PlatformLoginRequest): Observable<PlatformLoginResponse> {
    return this.http.post<PlatformLoginResponse>('platform/auth/login', request);
  }
}
