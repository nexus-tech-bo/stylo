import { Dialog } from '@angular/cdk/dialog';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { filter, map, of, switchMap } from 'rxjs';
import { PlatformApi } from '../../core/api/platform.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { PlatformBusiness } from '../../core/models/api.models';
import { businessStatusLabel, initials } from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SegmentedTabsComponent } from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { CreateBusinessDialogComponent } from './create-business-dialog.component';

/** SUPERADMIN. Search and status filter are client-side (endpoint not paged). */
@Component({
  selector: 'app-businesses-page',
  imports: [
    DatePipe,
    MoneyPipe,
    BadgeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    SegmentedTabsComponent,
    UiIconComponent,
  ],
  templateUrl: './businesses-page.component.html',
})
export class BusinessesPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly api = inject(PlatformApi);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  protected readonly statusLabel = businessStatusLabel;
  protected readonly initials = initials;
  protected readonly statusFilters = [
    { id: 'all', label: 'Todos' },
    { id: 'active', label: 'Activos' },
    { id: 'suspended', label: 'Suspendidos' },
  ];
  protected readonly search = signal('');
  protected readonly status = signal('all');
  protected readonly busyId = signal<number | null>(null);

  protected readonly businesses = rxResource({ stream: () => this.api.businesses() });
  protected readonly rows = computed(() => {
    if (!this.businesses.hasValue()) return [];
    const q = this.search().trim().toLowerCase();
    const status = this.status();
    return this.businesses
      .value()
      .filter((b) => status === 'all' || b.active === (status === 'active'))
      .filter((b) => !q || b.name.toLowerCase().includes(q) || b.code.includes(q));
  });

  protected create(): void {
    this.dialog
      .open<PlatformBusiness>(CreateBusinessDialogComponent)
      .closed.pipe(filter(Boolean))
      .subscribe((created) => {
        this.notifications.success(`Negocio «${created.name}» creado. Código: ${created.code}`);
        this.businesses.update((list) => [created, ...(list ?? [])]);
      });
  }

  protected toggle(business: PlatformBusiness): void {
    const active = !business.active;
    const confirmed$ = active
      ? of(true)
      : this.confirm.ask({
          title: 'Suspender negocio',
          message: `«${business.name}» y todo su equipo perderán el acceso de inmediato. Puedes reactivarlo cuando quieras.`,
          confirmLabel: 'Suspender',
          danger: true,
        });
    confirmed$
      .pipe(
        filter(Boolean),
        switchMap(() => {
          this.busyId.set(business.id);
          return this.api.setStatus(business.id, active).pipe(map(() => active));
        }),
      )
      .subscribe({
        next: () => {
          this.busyId.set(null);
          this.businesses.update((list) =>
            list?.map((b) => (b.id === business.id ? { ...b, active } : b)),
          );
          this.notifications.success(active ? 'Negocio reactivado' : 'Negocio suspendido');
        },
        error: (e) => {
          this.busyId.set(null);
          this.notifications.error(apiErrorMessage(e));
        },
      });
  }
}
