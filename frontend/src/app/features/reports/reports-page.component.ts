import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ReportsApi } from '../../core/api/reports.api';
import { SessionStore } from '../../core/auth/session.store';
import { apiError } from '../../core/http/api-error';
import { ItemType } from '../../core/models/api.models';
import { DateRange, parseLocalDate, rangeError, today } from '../../core/utils/dates.util';
import { PAYMENT_LABELS } from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { NotificationService } from '../../shared/services/notification.service';
import { DateRangeComponent } from '../../shared/ui/date-range/date-range.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SegmentedTabsComponent } from '../../shared/ui/segmented-tabs/segmented-tabs.component';

/** ?from&to. CASHIER sees only the summary; chart, stylists, top items and CSV are ADMIN-only (not requested otherwise). */
@Component({
  selector: 'app-reports-page',
  imports: [
    DatePipe,
    MoneyPipe,
    DateRangeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    SegmentedTabsComponent,
    UiIconComponent,
  ],
  templateUrl: './reports-page.component.html',
})
export class ReportsPageComponent {
  private readonly router = inject(Router);
  private readonly api = inject(ReportsApi);
  private readonly notifications = inject(NotificationService);

  readonly from = input<string>();
  readonly to = input<string>();

  protected readonly isAdmin = inject(SessionStore).isAdmin;
  protected readonly paymentLabels = PAYMENT_LABELS;
  protected readonly topTabs = [
    { id: 'SERVICE', label: 'Servicios' },
    { id: 'PRODUCT', label: 'Productos' },
  ];

  protected readonly range = computed<DateRange>(() => ({
    from: this.from() || today(),
    to: this.to() || today(),
  }));
  private readonly clientRangeError = computed(() => rangeError(this.range(), true));
  private readonly validRange = computed(() =>
    this.clientRangeError() ? undefined : this.range(),
  );
  private readonly adminRange = computed(() => (this.isAdmin() ? this.validRange() : undefined));

  protected readonly summary = rxResource({
    params: () => this.validRange(),
    stream: ({ params }) => this.api.summary(params),
  });
  protected readonly byDay = rxResource({
    params: () => this.adminRange(),
    stream: ({ params }) => this.api.salesByDay(params),
  });
  protected readonly stylists = rxResource({
    params: () => this.adminRange(),
    stream: ({ params }) => this.api.stylists(params),
  });
  protected readonly topType = signal<ItemType>('SERVICE');
  protected readonly top = rxResource({
    params: () =>
      this.adminRange() ? { type: this.topType(), range: this.adminRange()! } : undefined,
    stream: ({ params }) => this.api.top(params.type, params.range),
  });

  /** Client check first; a server 422 (range rule) shows in the same spot. */
  protected readonly rangeMessage = computed(() => {
    if (this.clientRangeError()) return this.clientRangeError();
    const info = apiError(this.summary.error());
    return info.status === 422 ? info.message : '';
  });

  protected readonly showTable = signal(false);
  protected readonly exporting = signal(false);

  protected readonly days = computed(() =>
    this.byDay.hasValue()
      ? this.byDay.value().map((d) => ({ ...d, day: parseLocalDate(d.date) }))
      : [],
  );
  protected readonly maxDayTotal = computed(() => Math.max(1, ...this.days().map((d) => d.total)));
  /** Label every Nth bar so long ranges stay readable. */
  protected readonly labelEvery = computed(() => Math.max(1, Math.ceil(this.days().length / 15)));
  protected readonly maxMethodTotal = computed(() =>
    this.summary.hasValue()
      ? Math.max(1, ...this.summary.value().byPaymentMethod.map((m) => m.total))
      : 1,
  );

  protected setRange(range: DateRange): void {
    void this.router.navigate([], { queryParams: range, queryParamsHandling: 'merge' });
  }

  protected exportCsv(): void {
    const range = this.validRange();
    if (!range) return;
    this.exporting.set(true);
    this.api
      .salesCsv(range)
      .pipe(finalize(() => this.exporting.set(false)))
      .subscribe({
        next: (response) => {
          const disposition = response.headers.get('Content-Disposition') ?? '';
          const filename =
            /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition)?.[1] ??
            `ventas_${range.from}_${range.to}.csv`;
          const url = URL.createObjectURL(response.body ?? new Blob());
          const link = document.createElement('a');
          link.href = url;
          link.download = decodeURIComponent(filename);
          link.click();
          URL.revokeObjectURL(url);
        },
        error: () => this.notifications.error('No se pudo exportar el CSV.'),
      });
  }
}
