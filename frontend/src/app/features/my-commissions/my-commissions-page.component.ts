import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { ReportsApi } from '../../core/api/reports.api';
import { SessionStore } from '../../core/auth/session.store';
import { apiError } from '../../core/http/api-error';
import { DateRange, presetRange, rangeError } from '../../core/utils/dates.util';
import { roundMoney } from '../../core/utils/forms.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { DateRangeComponent } from '../../shared/ui/date-range/date-range.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';

/** STYLIST. ?from&to, default "Este mes" (sent explicitly). */
@Component({
  selector: 'app-my-commissions-page',
  imports: [MoneyPipe, DateRangeComponent, EmptyStateComponent, PageHeaderComponent],
  templateUrl: './my-commissions-page.component.html',
})
export class MyCommissionsPageComponent {
  private readonly router = inject(Router);
  private readonly api = inject(ReportsApi);
  private readonly session = inject(SessionStore);

  readonly from = input<string>();
  readonly to = input<string>();

  protected readonly range = computed<DateRange>(() => {
    const month = presetRange('thisMonth');
    return { from: this.from() || month.from, to: this.to() || month.to };
  });
  private readonly clientRangeError = computed(() => rangeError(this.range(), true));

  protected readonly commissions = rxResource({
    params: () => (this.clientRangeError() ? undefined : this.range()),
    stream: ({ params }) => this.api.myCommissions(params),
  });
  protected readonly rangeMessage = computed(() => {
    if (this.clientRangeError()) return this.clientRangeError();
    const info = apiError(this.commissions.error());
    return info.status === 422 ? info.message : '';
  });

  /** Usually one row (own user); summed defensively. */
  protected readonly totals = computed(() => {
    if (!this.commissions.hasValue()) return null;
    const rows = this.commissions.value();
    if (!rows.length) return { empty: true, services: 0, total: 0, commission: 0, rate: 0 };
    return {
      empty: false,
      services: rows.reduce((n, r) => n + r.services, 0),
      total: roundMoney(rows.reduce((n, r) => n + r.total, 0)),
      commission: roundMoney(rows.reduce((n, r) => n + r.commission, 0)),
      rate: rows[0].commissionRate ?? this.session.user()?.commissionRate ?? 0,
    };
  });

  protected setRange(range: DateRange): void {
    void this.router.navigate([], { queryParams: range, queryParamsHandling: 'merge' });
  }
}
