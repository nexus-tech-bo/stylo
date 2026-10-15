import { ListDatePipe } from '../../shared/pipes/list-date.pipe';
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { CustomersApi } from '../../core/api/customers.api';
import { SalesApi } from '../../core/api/sales.api';
import { apiErrorMessage } from '../../core/http/api-error';
import { PAGE_SIZE, pageIndex } from '../../core/http/http-params';
import { DateRange, rangeError, today } from '../../core/utils/dates.util';
import { PAYMENT_LABELS, SALE_STATUS_LABELS } from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { DateRangeComponent } from '../../shared/ui/date-range/date-range.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';

/** Filters live in the query string: ?from&to&customerId&page. */
@Component({
  selector: 'app-sales-list-page',
  imports: [
    ListDatePipe,
    RouterLink,
    MoneyPipe,
    BadgeComponent,
    DateRangeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    PaginationComponent,
    UiIconComponent,
  ],
  templateUrl: './sales-list-page.component.html',
})
export class SalesListPageComponent {
  private readonly router = inject(Router);
  private readonly salesApi = inject(SalesApi);
  private readonly customersApi = inject(CustomersApi);

  readonly from = input<string>();
  readonly to = input<string>();
  readonly customerId = input<string>();
  readonly page = input<string>();

  protected readonly paymentLabels = PAYMENT_LABELS;
  protected readonly statusLabels = SALE_STATUS_LABELS;

  protected readonly range = computed<DateRange>(() => ({
    from: this.from() || today(),
    to: this.to() || today(),
  }));
  protected readonly rangeError = computed(() => rangeError(this.range()));
  protected readonly isToday = computed(
    () => this.range().from === today() && this.range().to === today(),
  );
  private readonly customerIdNumber = computed(() =>
    this.customerId() ? Number(this.customerId()) : undefined,
  );

  protected readonly sales = rxResource({
    params: () =>
      this.rangeError()
        ? undefined
        : {
            ...this.range(),
            customerId: this.customerIdNumber(),
            page: pageIndex(this.page()),
            size: PAGE_SIZE,
          },
    stream: ({ params }) => this.salesApi.list(params),
  });
  protected readonly customer = rxResource({
    params: () => this.customerIdNumber(),
    stream: ({ params }) => this.customersApi.get(params),
  });
  protected readonly rows = computed(() =>
    this.sales.hasValue() ? this.sales.value().content : [],
  );
  protected readonly errorMessage = computed(() =>
    this.sales.error()
      ? apiErrorMessage(this.sales.error(), 'No se pudieron cargar las ventas.')
      : '',
  );

  private navigate(queryParams: Record<string, string | number | null>): void {
    void this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }

  protected setRange(range: DateRange): void {
    this.navigate({ ...range, page: null });
  }

  protected setPage(page: number): void {
    this.navigate({ page });
  }

  protected clearCustomer(): void {
    this.navigate({ customerId: null, page: null });
  }

  protected open(id: number): void {
    void this.router.navigate(['/ventas', id]);
  }
}
