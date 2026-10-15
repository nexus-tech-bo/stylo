import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { SalesApi } from '../../core/api/sales.api';
import { SaleResponse } from '../../core/models/api.models';
import { formErrorBanner } from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';

/** ADMIN. Closes with the voided sale. */
@Component({
  selector: 'app-void-sale-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR],
  template: `
    <app-dialog-frame
      [title]="'Anular venta #' + sale.id"
      subtitle="Esta acción no se puede deshacer. El stock de los productos se repone."
      (closed)="ref.close()"
    >
      <form id="void-form" [formGroup]="form" (ngSubmit)="save()" class="space-y-4" novalidate>
        @if (error()) {
          <p class="form-banner" role="alert">{{ error() }}</p>
        }
        <div>
          <label class="field-label" for="void-reason">Motivo *</label>
          <textarea
            id="void-reason"
            rows="3"
            class="field-input"
            formControlName="reason"
            maxlength="250"
          ></textarea>
          <p class="field-hint text-right">{{ form.controls.reason.value.length }}/250</p>
          <app-field-error [control]="form.controls.reason" />
        </div>
      </form>
      <div dialogFooter class="contents">
        <button type="button" class="btn btn-secondary" cdkFocusInitial (click)="ref.close()">
          Cancelar
        </button>
        <button type="submit" form="void-form" class="btn btn-danger" [disabled]="saving()">
          {{ saving() ? 'Anulando…' : 'Anular venta' }}
        </button>
      </div>
    </app-dialog-frame>
  `,
})
export class VoidSaleDialogComponent {
  private readonly api = inject(SalesApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  protected readonly sale = inject<SaleResponse>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<SaleResponse>>(DialogRef);

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    reason: ['', [Validators.required, Validators.maxLength(250)]],
  });

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    this.saving.set(true);
    this.error.set('');
    this.api
      .voidSale(this.sale.id, this.form.getRawValue().reason.trim())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (voided) => this.ref.close(voided),
        error: (e) =>
          this.error.set(
            formErrorBanner(
              this.form,
              e,
              'No se pudo anular: solo se pueden anular ventas de la caja abierta.',
            ),
          ),
      });
  }
}
