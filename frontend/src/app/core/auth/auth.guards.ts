import { inject } from '@angular/core';
import { CanActivateFn, CanMatchFn, RedirectFunction, Router } from '@angular/router';
import { Role } from '../models/api.models';
import { homePath, PLATFORM_HOME } from '../navigation/navigation';
import { SessionStore } from './session.store';

function loginRedirect(platform: boolean) {
  const session = inject(SessionStore);
  const hadSession = platform ? !!session.platform() : !!session.business();
  const returnUrl = inject(Router).currentNavigation()?.extractedUrl.toString();
  return session.endSession(platform, hadSession ? 'expired' : undefined, returnUrl);
}

export const businessAuthGuard: CanMatchFn = () =>
  inject(SessionStore).hasBusinessSession() || loginRedirect(false);

export const platformAuthGuard: CanMatchFn = () =>
  inject(SessionStore).hasPlatformSession() || loginRedirect(true);

/** `data.roles`; a disallowed role goes to its own home. */
export const roleGuard: CanActivateFn = (route) => {
  const role = inject(SessionStore).role();
  const roles = route.data['roles'] as readonly Role[];
  return (!!role && roles.includes(role)) || inject(Router).createUrlTree([homePath(role)]);
};

/** Login pages: already signed in → home of that area. `data.platform` selects the area. */
export const loginGuard: CanActivateFn = (route) => {
  const session = inject(SessionStore);
  const router = inject(Router);
  if (route.data['platform'])
    return !session.hasPlatformSession() || router.createUrlTree([PLATFORM_HOME]);
  return !session.hasBusinessSession() || router.createUrlTree([homePath(session.role())]);
};

export const homeRedirect: RedirectFunction = () => homePath(inject(SessionStore).role());
