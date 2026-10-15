import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CustomersApi } from '../../../core/api/customers.api';
import { CustomerRequest, CustomerResponse } from '../../../core/models/api.models';
import { formErrorBanner } from '../../../core/utils/forms.util';
import { rejectInvalid } from '../../utils/forms.util';
import { NotificationService } from '../../services/notification.service';
import { DialogFrameComponent } from '../../ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../ui/field-error/field-error.component';

/** Create/edit a customer. Used by Clientes and the POS customer picker. Closes with the saved customer. */
@Component({
  selector: 'app-customer-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR],
  templateUrl: './customer-dialog.component.html',
})
export class CustomerDialogComponent {
  private readonly api = inject(CustomersApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly notifications = inject(NotificationService);
  protected readonly ref = inject<DialogRef<CustomerResponse>>(DialogRef);
  protected readonly customer = inject<CustomerResponse | null>(DIALOG_DATA) ?? null;

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.customer?.name ?? '', [Validators.required, Validators.maxLength(120)]],
    phone: [this.customer?.phone ?? '', Validators.maxLength(30)],
    email: [this.customer?.email ?? '', [Validators.email, Validators.maxLength(120)]],
    taxId: [this.customer?.taxId ?? '', Validators.maxLength(30)],
    notes: [this.customer?.notes ?? '', Validators.maxLength(500)],
  });

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    const request: CustomerRequest = {
      name: v.name.trim(),
      phone: v.phone.trim() || null,
      email: v.email.trim() || null,
      taxId: v.taxId.trim() || null,
      notes: v.notes.trim() || null,
    };
    this.saving.set(true);
    this.error.set('');
    const request$ = this.customer
      ? this.api.update(this.customer.id, request)
      : this.api.create(request);
    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => {
        this.notifications.success('Cliente guardado');
        this.ref.close(saved);
      },
      error: (e) => this.error.set(formErrorBanner(this.form, e)),
    });
  }
}
