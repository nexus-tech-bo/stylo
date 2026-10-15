import { Dialog } from '@angular/cdk/dialog';
import { DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { CustomersApi } from '../../core/api/customers.api';
import { CustomerResponse } from '../../core/models/api.models';
import { PAGE_SIZE, pageIndex } from '../../core/http/http-params';
import { today, toLocalDate } from '../../core/utils/dates.util';
import { initials } from '../../core/utils/labels.util';
import { CustomerDialogComponent } from '../../shared/dialogs/customer-dialog/customer-dialog.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';

/** ?q&page in the query string; search debounced 300 ms. */
@Component({
  selector: 'app-customers-page',
  imports: [
    DatePipe,
    RouterLink,
    EmptyStateComponent,
    PageHeaderComponent,
    PaginationComponent,
    UiIconComponent,
  ],
  templateUrl: './customers-page.component.html',
})
export class CustomersPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly api = inject(CustomersApi);
  private searchTimer?: ReturnType<typeof setTimeout>;

  readonly q = input<string>();
  readonly page = input<string>();

  protected readonly initials = initials;
  protected readonly customers = rxResource({
    params: () => ({
      q: this.q()?.trim() || undefined,
      page: pageIndex(this.page()),
      size: PAGE_SIZE,
    }),
    stream: ({ params }) => this.api.list(params),
  });
  protected readonly rows = computed(() =>
    this.customers.hasValue() ? this.customers.value().content : [],
  );

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.searchTimer));
  }

  protected onSearch(value: string): void {
    clearTimeout(this.searchTimer);
    this.searchTimer = setTimeout(
      () => this.navigate({ q: value.trim() || null, page: null }),
      300,
    );
  }

  protected clearSearch(): void {
    this.navigate({ q: null, page: null });
  }

  protected setPage(page: number): void {
    this.navigate({ page });
  }

  protected salesLink(c: CustomerResponse): Record<string, string | number> {
    return { customerId: c.id, from: toLocalDate(new Date(c.createdAt)), to: today() };
  }

  protected edit(customer: CustomerResponse | null): void {
    this.dialog
      .open<CustomerResponse>(CustomerDialogComponent, { data: customer })
      .closed.pipe(filter(Boolean))
      .subscribe(() => this.customers.reload());
  }

  private navigate(queryParams: Record<string, string | number | null>): void {
    void this.router.navigate([], { queryParams, queryParamsHandling: 'merge' });
  }
}
