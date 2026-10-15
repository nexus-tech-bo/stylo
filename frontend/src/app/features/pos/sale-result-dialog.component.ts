import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, inject } from '@angular/core';
import { SaleResponse } from '../../core/models/api.models';
import { PAYMENT_LABELS } from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';

/** Closes with 'detail' to open the sale; anything else = keep selling (cart already reset). */
@Component({
  selector: 'app-sale-result-dialog',
  imports: [DialogFrameComponent, MoneyPipe, UiIconComponent],
  template: `
    <app-dialog-frame
      title="Venta registrada"
      [subtitle]="'Venta #' + sale.id"
      (closed)="ref.close()"
    >
      <div class="text-center">
        <span
          class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-emerald-600"
        >
          <app-ui-icon name="check" class="h-7 w-7" [strokeWidth]="2.6" />
        </span>
        <p class="text-sm text-gray-500">Total · {{ paymentLabels[sale.paymentMethod] }}</p>
        <p class="text-2xl font-extrabold text-gray-900 tabular-nums">{{ sale.total | money }}</p>
        @if (sale.paymentMethod === 'CASH' && sale.change !== null) {
          <p class="mt-4 text-sm font-semibold text-gray-500">Cambio</p>
          <p class="text-5xl font-extrabold text-primary tabular-nums">{{ sale.change | money }}</p>
        }
      </div>
      <div dialogFooter class="contents">
        <button type="button" class="btn btn-secondary" (click)="ref.close('detail')">
          Ver detalle
        </button>
        <button type="button" class="btn btn-primary" cdkFocusInitial (click)="ref.close()">
          Nueva venta
        </button>
      </div>
    </app-dialog-frame>
  `,
})
export class SaleResultDialogComponent {
  protected readonly sale = inject<SaleResponse>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<'detail'>>(DialogRef);
  protected readonly paymentLabels = PAYMENT_LABELS;
}
