import { Dialog } from '@angular/cdk/dialog';
import { DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { CashRegisterApi } from '../../core/api/cash-register.api';
import { CatalogApi } from '../../core/api/catalog.api';
import { ReportsApi } from '../../core/api/reports.api';
import { PAYMENT_LABELS, stockLabel } from '../../core/utils/labels.util';
import { OpenRegisterDialogComponent } from '../../shared/dialogs/open-register-dialog/open-register-dialog.component';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-dashboard-page',
  imports: [
    DatePipe,
    RouterLink,
    MoneyPipe,
    BadgeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    UiIconComponent,
  ],
  templateUrl: './dashboard-page.component.html',
})
export class DashboardPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly registerApi = inject(CashRegisterApi);
  private readonly reportsApi = inject(ReportsApi);
  private readonly catalogApi = inject(CatalogApi);

  protected readonly now = new Date();
  protected readonly paymentLabels = PAYMENT_LABELS;
  protected readonly stockLabel = stockLabel;

  protected readonly register = rxResource({ stream: () => this.registerApi.current() });
  protected readonly summary = rxResource({ stream: () => this.reportsApi.summary() });
  protected readonly lowStock = rxResource({ stream: () => this.catalogApi.lowStock() });

  protected readonly maxMethodTotal = computed(() =>
    this.summary.hasValue()
      ? Math.max(1, ...this.summary.value().byPaymentMethod.map((m) => m.total))
      : 1,
  );

  protected openRegister(): void {
    this.dialog
      .open(OpenRegisterDialogComponent)
      .closed.pipe(filter(Boolean))
      .subscribe(() => this.register.reload());
  }
}
