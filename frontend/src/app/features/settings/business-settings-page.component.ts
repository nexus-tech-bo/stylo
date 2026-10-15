import { Component, ElementRef, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, tap } from 'rxjs';
import { BusinessApi } from '../../core/api/business.api';
import { SessionStore } from '../../core/auth/session.store';
import { BusinessRequest, BusinessResponse } from '../../core/models/api.models';
import { formErrorBanner } from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { MONEY_FORMAT } from '../../shared/pipes/money.pipe';
import { NotificationService } from '../../shared/services/notification.service';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';

/** ADMIN. */
@Component({
  selector: 'app-business-settings-page',
  imports: [ReactiveFormsModule, EmptyStateComponent, ...FIELD_ERROR, PageHeaderComponent],
  templateUrl: './business-settings-page.component.html',
})
export class BusinessSettingsPageComponent {
  private readonly api = inject(BusinessApi);
  private readonly session = inject(SessionStore);
  private readonly notifications = inject(NotificationService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    taxId: ['', Validators.maxLength(30)],
    address: ['', Validators.maxLength(200)],
    phone: ['', Validators.maxLength(30)],
    currency: ['', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
    currencySymbol: ['', [Validators.required, Validators.maxLength(5)]],
    taxRate: [0, [Validators.required, Validators.min(0), Validators.max(100)]],
    receiptMessage: ['', Validators.maxLength(250)],
  });

  /** Loaded values also reset the form (they are its pristine state). */
  protected readonly business = rxResource({
    stream: () => this.api.get().pipe(tap((b) => this.reset(b))),
  });

  protected preview(): string {
    return `${this.form.controls.currencySymbol.value} ${MONEY_FORMAT.format(1234.5)}`;
  }

  protected upperCurrency(): void {
    const control = this.form.controls.currency;
    control.setValue(control.value.toUpperCase(), { emitEvent: false });
  }

  protected discard(): void {
    if (this.business.hasValue()) this.reset(this.business.value());
  }

  private reset(b: BusinessResponse): void {
    this.form.reset({
      name: b.name,
      taxId: b.taxId ?? '',
      address: b.address ?? '',
      phone: b.phone ?? '',
      currency: b.currency,
      currencySymbol: b.currencySymbol,
      taxRate: b.taxRate,
      receiptMessage: b.receiptMessage ?? '',
    });
    this.error.set('');
  }

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    const request: BusinessRequest = {
      name: v.name.trim(),
      taxId: v.taxId.trim() || null,
      address: v.address.trim() || null,
      phone: v.phone.trim() || null,
      currency: v.currency,
      currencySymbol: v.currencySymbol.trim(),
      taxRate: v.taxRate,
      receiptMessage: v.receiptMessage.trim() || null,
    };
    this.saving.set(true);
    this.error.set('');
    this.api
      .update(request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (saved) => {
          this.business.set(saved);
          this.reset(saved);
          this.session.settings.set(saved);
          this.notifications.success('Ajustes guardados');
        },
        error: (e) => this.error.set(formErrorBanner(this.form, e)),
      });
  }
}
