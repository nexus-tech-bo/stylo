import { Dialog } from '@angular/cdk/dialog';
import { Component, computed, inject, input, output } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessionStore } from '../../core/auth/session.store';
import { BUSINESS_NAV, PLATFORM_NAV } from '../../core/navigation/navigation';
import { initials, ROLE_LABELS } from '../../core/utils/labels.util';
import { ChangePasswordDialogComponent } from '../../shared/dialogs/change-password-dialog/change-password-dialog.component';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';

@Component({
  selector: 'app-sidebar-nav',
  imports: [RouterLink, RouterLinkActive, UiIconComponent, BadgeComponent],
  templateUrl: './sidebar-nav.component.html',
  host: { class: 'flex-col p-4' },
})
export class SidebarNavComponent {
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(Dialog);

  readonly platform = input(false);
  readonly navigated = output<void>();

  protected readonly user = this.session.user;
  protected readonly businessName = this.session.businessName;
  protected readonly platformName = computed(() => this.session.platform()?.name ?? '');
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly initials = initials;
  protected readonly items = computed(() => {
    if (this.platform()) return PLATFORM_NAV;
    const role = this.session.role();
    return BUSINESS_NAV.filter((item) => !!role && item.roles.includes(role));
  });

  protected changePassword(): void {
    this.navigated.emit();
    this.dialog.open(ChangePasswordDialogComponent);
  }

  protected logout(): void {
    this.navigated.emit();
    void this.router.navigateByUrl(this.session.endSession(this.platform()));
  }
}
