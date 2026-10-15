import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CashRegisterApi } from '../../core/api/cash-register.api';
import { SessionStore } from '../../core/auth/session.store';
import { CashRegisterResponse } from '../../core/models/api.models';
import {
  formErrorBanner,
  moneyValidator,
  parseMoney,
  roundMoney,
} from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { differenceLabel, LabelTone } from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';

/** The dialog itself is the confirmation. Closes with the closed register. */
@Component({
  selector: 'app-close-register-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR, MoneyPipe, BadgeComponent],
  templateUrl: './close-register-dialog.component.html',
})
export class CloseRegisterDialogComponent {
  private readonly api = inject(CashRegisterApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  protected readonly register = inject<CashRegisterResponse>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<CashRegisterResponse>>(DialogRef);
  protected readonly symbol = inject(SessionStore).currencySymbol;

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    countedCash: ['', [Validators.required, moneyValidator(0)]],
    notes: ['', Validators.maxLength(500)],
  });

  /** Live difference; read from the template (re-evaluated on each input event). */
  protected difference(): { value: number; label: LabelTone } | null {
    const counted = parseMoney(this.form.controls.countedCash.value);
    if (counted === null) return null;
    const value = roundMoney(counted - this.register.expectedCash);
    return { value, label: differenceLabel(value) };
  }

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');
    this.api
      .close({ countedCash: parseMoney(v.countedCash) ?? 0, notes: v.notes.trim() || null })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (closed) => this.ref.close(closed),
        error: (e) => this.error.set(formErrorBanner(this.form, e)),
      });
  }
}
