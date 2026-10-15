import { Dialog } from '@angular/cdk/dialog';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { UsersApi } from '../../core/api/users.api';
import { SessionStore } from '../../core/auth/session.store';
import { UserResponse } from '../../core/models/api.models';
import { activeLabel, initials, ROLE_LABELS } from '../../core/utils/labels.util';
import { NotificationService } from '../../shared/services/notification.service';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SegmentedTabsComponent } from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { UserDialogComponent, UserDialogData } from './user-dialog.component';

/** ADMIN. Role filter is client-side (endpoint not paged). */
@Component({
  selector: 'app-users-page',
  imports: [
    BadgeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    SegmentedTabsComponent,
    UiIconComponent,
  ],
  templateUrl: './users-page.component.html',
})
export class UsersPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly api = inject(UsersApi);
  private readonly session = inject(SessionStore);
  private readonly notifications = inject(NotificationService);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly activeLabel = activeLabel;
  protected readonly initials = initials;
  protected readonly filters = [
    { id: 'ALL', label: 'Todos' },
    { id: 'ADMIN', label: 'Administradores' },
    { id: 'CASHIER', label: 'Cajeros' },
    { id: 'STYLIST', label: 'Estilistas' },
  ];
  protected readonly roleFilter = signal('ALL');

  protected readonly users = rxResource({ stream: () => this.api.list() });
  protected readonly rows = computed(() => {
    if (!this.users.hasValue()) return [];
    const role = this.roleFilter();
    return this.users.value().filter((u) => role === 'ALL' || u.role === role);
  });

  protected edit(user: UserResponse | null): void {
    const data: UserDialogData = { user, selfId: this.session.user()?.id ?? null };
    this.dialog
      .open<UserResponse>(UserDialogComponent, { data })
      .closed.pipe(filter(Boolean))
      .subscribe((saved) => {
        this.notifications.success('Usuario guardado');
        if (saved.id === data.selfId) this.session.updateUser(saved);
        this.users.reload();
      });
  }
}
