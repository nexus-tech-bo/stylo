import { A11yModule } from '@angular/cdk/a11y';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { AuthApi } from '../../core/api/auth.api';
import { BusinessApi } from '../../core/api/business.api';
import { SessionStore } from '../../core/auth/session.store';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { SidebarNavComponent } from './sidebar-nav.component';

function deepestTitle(route: ActivatedRouteSnapshot): string {
  let current = route;
  while (current.firstChild) current = current.firstChild;
  return current.title ?? '';
}

/** Blob background + sidebar (drawer below lg) + outlet. Used by the business and platform areas (`data.platform`). */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, A11yModule, SidebarNavComponent, UiIconComponent],
  templateUrl: './shell.component.html',
})
export class ShellComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly session = inject(SessionStore);
  private readonly authApi = inject(AuthApi);
  private readonly businessApi = inject(BusinessApi);

  readonly platform = input(false);

  protected readonly menuOpen = signal(false);
  protected readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => deepestTitle(this.router.routerState.snapshot.root)),
    ),
    { initialValue: deepestTitle(this.router.routerState.snapshot.root) },
  );

  ngOnInit(): void {
    if (this.platform()) return;
    // Refresh the user (role/active may have changed) and load business settings (currency symbol for every amount).
    // Failures: 401/suspended are handled by the interceptor; otherwise amounts just render without symbol.
    this.authApi.me().subscribe({
      next: (user) => this.session.updateUser(user),
      error: () => undefined,
    });
    this.businessApi.get().subscribe({
      next: (settings) => this.session.settings.set(settings),
      error: () => undefined,
    });
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }
}
