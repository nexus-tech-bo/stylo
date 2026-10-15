import { DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CashRegisterApi } from '../../../core/api/cash-register.api';
import { SessionStore } from '../../../core/auth/session.store';
import { CashRegisterResponse } from '../../../core/models/api.models';
import { formErrorBanner, moneyValidator, parseMoney } from '../../../core/utils/forms.util';
import { rejectInvalid } from '../../utils/forms.util';
import { NotificationService } from '../../services/notification.service';
import { DialogFrameComponent } from '../../ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../ui/field-error/field-error.component';

/** Opens the register. Used by Inicio, Nueva venta and Caja. Closes with the opened register. */
@Component({
  selector: 'app-open-register-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR],
  templateUrl: './open-register-dialog.component.html',
})
export class OpenRegisterDialogComponent {
  private readonly api = inject(CashRegisterApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly notifications = inject(NotificationService);
  protected readonly ref = inject<DialogRef<CashRegisterResponse>>(DialogRef);
  protected readonly symbol = inject(SessionStore).currencySymbol;

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    openingAmount: ['', [Validators.required, moneyValidator(0)]],
    notes: ['', Validators.maxLength(500)],
  });

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');
    this.api
      .open({ openingAmount: parseMoney(v.openingAmount) ?? 0, notes: v.notes.trim() || null })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (register) => {
          this.notifications.success('Caja abierta');
          this.ref.close(register);
        },
        error: (e) => this.error.set(formErrorBanner(this.form, e)),
      });
  }
}
