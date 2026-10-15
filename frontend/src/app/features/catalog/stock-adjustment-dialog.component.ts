import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { CatalogApi } from '../../core/api/catalog.api';
import { ProductResponse } from '../../core/models/api.models';
import { formErrorBanner } from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';

const integer: ValidatorFn = (c) =>
  c.value !== null && !Number.isInteger(c.value) ? { integer: 'Ingresa un número entero.' } : null;

/** ADMIN. Sends a signed quantity. Closes with the updated product. */
@Component({
  selector: 'app-stock-adjustment-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR],
  template: `
    <app-dialog-frame
      [title]="'Ajustar stock · ' + product.name"
      [subtitle]="'Stock actual ' + product.stock"
      (closed)="ref.close()"
    >
      <form id="stock-form" [formGroup]="form" (ngSubmit)="save()" class="space-y-4" novalidate>
        @if (error()) {
          <p class="form-banner" role="alert">{{ error() }}</p>
        }
        <fieldset>
          <legend class="field-label">Movimiento</legend>
          <div class="grid grid-cols-2 gap-2">
            <label class="choice-card"
              ><input type="radio" class="sr-only" formControlName="direction" value="IN" /> Entrada
              (+)</label
            >
            <label class="choice-card"
              ><input type="radio" class="sr-only" formControlName="direction" value="OUT" /> Salida
              (−)</label
            >
          </div>
        </fieldset>
        <div>
          <label class="field-label" for="stock-quantity">Cantidad *</label>
          <input
            id="stock-quantity"
            type="number"
            min="1"
            step="1"
            class="field-input"
            formControlName="quantity"
            cdkFocusInitial
          />
          <app-field-error [control]="form.controls.quantity" />
        </div>
        @if (preview() !== null) {
          <p class="rounded-2xl bg-white/50 px-4 py-3 text-sm" aria-live="polite">
            Nuevo stock: <b class="text-gray-900 tabular-nums">{{ preview() }}</b>
          </p>
        }
      </form>
      <div dialogFooter class="contents">
        <button type="button" class="btn btn-secondary" (click)="ref.close()">Cancelar</button>
        <button type="submit" form="stock-form" class="btn btn-primary" [disabled]="saving()">
          {{ saving() ? 'Guardando…' : 'Aplicar ajuste' }}
        </button>
      </div>
    </app-dialog-frame>
  `,
})
export class StockAdjustmentDialogComponent {
  private readonly api = inject(CatalogApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  protected readonly product = inject<ProductResponse>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<ProductResponse>>(DialogRef);

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly notBelowZero: ValidatorFn = (control) =>
    control.parent?.get('direction')?.value === 'OUT' && control.value > this.product.stock
      ? { stock: `No puedes retirar más de ${this.product.stock} unidades.` }
      : null;
  protected readonly form = this.fb.group({
    direction: this.fb.control<'IN' | 'OUT'>('IN'),
    quantity: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(1),
      integer,
      this.notBelowZero,
    ]),
  });

  constructor() {
    this.form.controls.direction.valueChanges.subscribe(() =>
      this.form.controls.quantity.updateValueAndValidity(),
    );
  }

  protected signedQuantity(): number {
    const { direction, quantity } = this.form.getRawValue();
    return (quantity ?? 0) * (direction === 'OUT' ? -1 : 1);
  }

  protected preview(): number | null {
    const quantity = this.form.controls.quantity;
    return quantity.value && quantity.valid ? this.product.stock + this.signedQuantity() : null;
  }

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    this.saving.set(true);
    this.error.set('');
    this.api
      .adjustStock(this.product.id, this.signedQuantity())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (updated) => this.ref.close(updated),
        error: (e) => this.error.set(formErrorBanner(this.form, e)),
      });
  }
}
