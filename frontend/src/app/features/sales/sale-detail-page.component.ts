import { Dialog } from '@angular/cdk/dialog';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { CashRegisterApi } from '../../core/api/cash-register.api';
import { SalesApi } from '../../core/api/sales.api';
import { SessionStore } from '../../core/auth/session.store';
import { apiError } from '../../core/http/api-error';
import { SaleResponse } from '../../core/models/api.models';
import { PAYMENT_LABELS, SALE_STATUS_LABELS } from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { NotificationService } from '../../shared/services/notification.service';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { VoidSaleDialogComponent } from './void-sale-dialog.component';

@Component({
  selector: 'app-sale-detail-page',
  imports: [DatePipe, RouterLink, MoneyPipe, BadgeComponent, EmptyStateComponent, UiIconComponent],
  templateUrl: './sale-detail-page.component.html',
})
export class SaleDetailPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly salesApi = inject(SalesApi);
  private readonly registerApi = inject(CashRegisterApi);
  private readonly notifications = inject(NotificationService);

  readonly id = input.required<string>();

  protected readonly isAdmin = inject(SessionStore).isAdmin;
  protected readonly paymentLabels = PAYMENT_LABELS;
  protected readonly statusLabels = SALE_STATUS_LABELS;

  protected readonly sale = rxResource({
    params: () => Number(this.id()),
    stream: ({ params }) => this.salesApi.get(params),
  });
  /** Only ADMIN can void, so only ADMIN needs to know the open register. */
  protected readonly register = rxResource({
    params: () => (this.isAdmin() ? true : undefined),
    stream: () => this.registerApi.current(),
  });
  protected readonly notFound = computed(() => apiError(this.sale.error()).status === 404);
  protected readonly canVoid = computed(() => {
    if (!this.sale.hasValue() || !this.register.hasValue()) return false;
    return this.register.value()?.id === this.sale.value().cashRegisterId;
  });

  protected openVoid(sale: SaleResponse): void {
    this.dialog
      .open<SaleResponse>(VoidSaleDialogComponent, { data: sale })
      .closed.pipe(filter(Boolean))
      .subscribe((voided) => {
        this.notifications.success('Venta anulada');
        this.sale.set(voided);
      });
  }
}
