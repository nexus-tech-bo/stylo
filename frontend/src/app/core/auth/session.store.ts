import { computed, inject, Injectable, signal } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import {
  BusinessResponse,
  LoginResponse,
  PlatformLoginResponse,
  UserResponse,
} from '../models/api.models';

export type BusinessSession = LoginResponse;
export type PlatformSession = PlatformLoginResponse;

const BUSINESS_KEY = 'sf.business';
const PLATFORM_KEY = 'sf.platform';
const LAST_CODE_KEY = 'sf.lastBusinessCode';

function read<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

function write(key: string, value: unknown): void {
  try {
    if (value) localStorage.setItem(key, JSON.stringify(value));
    else localStorage.removeItem(key);
  } catch {
    // Storage unavailable (private mode): the session just won't survive a reload.
  }
}

const isValid = (s: { expiresAt: string } | null): boolean =>
  !!s && new Date(s.expiresAt).getTime() > Date.now();

/** Both sessions (business + platform) and the business settings needed app-wide, as signals. */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  private readonly router = inject(Router);
  private readonly _business = signal(read<BusinessSession>(BUSINESS_KEY));
  private readonly _platform = signal(read<PlatformSession>(PLATFORM_KEY));

  readonly business = this._business.asReadonly();
  readonly platform = this._platform.asReadonly();
  /** GET /api/business, loaded by the shell. */
  readonly settings = signal<BusinessResponse | null>(null);

  readonly user = computed(() => this._business()?.user ?? null);
  readonly role = computed(() => this.user()?.role ?? null);
  readonly isAdmin = computed(() => this.role() === 'ADMIN');
  readonly currencySymbol = computed(() => this.settings()?.currencySymbol ?? '');
  readonly businessName = computed(
    () => this.settings()?.name ?? this._business()?.business.name ?? '',
  );

  hasBusinessSession(): boolean {
    return isValid(this._business());
  }

  hasPlatformSession(): boolean {
    return isValid(this._platform());
  }

  setBusiness(session: BusinessSession): void {
    this._business.set(session);
    write(BUSINESS_KEY, session);
  }

  updateUser(user: UserResponse): void {
    const current = this._business();
    if (current) this.setBusiness({ ...current, user });
  }

  clearBusiness(): void {
    this._business.set(null);
    this.settings.set(null);
    write(BUSINESS_KEY, null);
  }

  setPlatform(session: PlatformSession): void {
    this._platform.set(session);
    write(PLATFORM_KEY, session);
  }

  clearPlatform(): void {
    this._platform.set(null);
    write(PLATFORM_KEY, null);
  }

  /** Clears that area's session and returns its login URL (`returnUrl` is kept for the business area only). */
  endSession(platform: boolean, reason?: 'expired' | 'suspended', returnUrl?: string): UrlTree {
    if (platform) this.clearPlatform();
    else this.clearBusiness();
    const queryParams: Record<string, string> = {};
    if (!platform && returnUrl && returnUrl !== '/') queryParams['returnUrl'] = returnUrl;
    if (reason) queryParams['reason'] = reason;
    return this.router.createUrlTree([platform ? '/plataforma/login' : '/login'], { queryParams });
  }

  /** Prefills the business login. */
  get lastBusinessCode(): string {
    return read<string>(LAST_CODE_KEY) ?? '';
  }

  set lastBusinessCode(code: string) {
    write(LAST_CODE_KEY, code);
  }
}
