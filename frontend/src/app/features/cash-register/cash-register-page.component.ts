import { Dialog, DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { CashRegisterApi } from '../../core/api/cash-register.api';
import { CashRegisterResponse } from '../../core/models/api.models';
import { PAGE_SIZE, pageIndex } from '../../core/http/http-params';
import { differenceLabel } from '../../core/utils/labels.util';
import { OpenRegisterDialogComponent } from '../../shared/dialogs/open-register-dialog/open-register-dialog.component';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { ListDatePipe } from '../../shared/pipes/list-date.pipe';
import { NotificationService } from '../../shared/services/notification.service';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { PaginationComponent } from '../../shared/ui/pagination/pagination.component';
import { CloseRegisterDialogComponent } from './close-register-dialog.component';
import { RegisterSummaryComponent } from './register-summary.component';

/** History row → read-only "Turno" view, no extra request. */
@Component({
  selector: 'app-register-detail-dialog',
  imports: [DatePipe, DialogFrameComponent, RegisterSummaryComponent],
  template: `
    <app-dialog-frame
      [title]="'Caja #' + register.id"
      [subtitle]="(register.openedAt | date: 'dd/MM/yyyy') ?? ''"
      (closed)="ref.close()"
    >
      <app-register-summary [register]="register" />
    </app-dialog-frame>
  `,
})
export class RegisterDetailDialogComponent {
  protected readonly register = inject<CashRegisterResponse>(DIALOG_DATA);
  protected readonly ref = inject(DialogRef);
}

@Component({
  selector: 'app-cash-register-page',
  imports: [
    ListDatePipe,
    MoneyPipe,
    BadgeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    PaginationComponent,
    RegisterSummaryComponent,
  ],
  providers: [MoneyPipe],
  templateUrl: './cash-register-page.component.html',
})
export class CashRegisterPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly api = inject(CashRegisterApi);
  private readonly notifications = inject(NotificationService);
  private readonly money = inject(MoneyPipe);

  readonly page = input<string>();

  protected readonly differenceLabel = differenceLabel;
  protected readonly current = rxResource({ stream: () => this.api.current() });
  protected readonly history = rxResource({
    params: () => ({ page: pageIndex(this.page()), size: PAGE_SIZE, sort: 'openedAt,desc' }),
    stream: ({ params }) => this.api.history(params),
  });
  protected readonly rows = computed(() =>
    this.history.hasValue() ? this.history.value().content : [],
  );

  protected openRegister(): void {
    this.dialog.open(OpenRegisterDialogComponent).closed.subscribe(() => this.current.reload());
  }

  protected closeRegister(register: CashRegisterResponse): void {
    this.dialog
      .open<CashRegisterResponse>(CloseRegisterDialogComponent, { data: register })
      .closed.subscribe((closed) => {
        if (closed?.difference !== null && closed?.difference !== undefined) {
          const label = differenceLabel(closed.difference).label;
          this.notifications.success(
            `Caja cerrada · Diferencia ${this.money.transform(closed.difference)} (${label})`,
          );
          this.history.reload();
        }
        this.current.reload();
      });
  }

  protected showDetail(register: CashRegisterResponse): void {
    this.dialog.open(RegisterDetailDialogComponent, { data: register });
  }

  protected setPage(page: number): void {
    void this.router.navigate([], { queryParams: { page }, queryParamsHandling: 'merge' });
  }
}
