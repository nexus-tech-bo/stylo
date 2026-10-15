import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { CatalogApi } from '../../core/api/catalog.api';
import { SessionStore } from '../../core/auth/session.store';
import {
  CategoryResponse,
  SalonServiceRequest,
  SalonServiceResponse,
} from '../../core/models/api.models';
import {
  formErrorBanner,
  moneyText,
  moneyValidator,
  parseMoney,
} from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';
import { ImageFieldComponent, imageErrorOf } from './image-field.component';

export interface ServiceDialogData {
  service: SalonServiceResponse | null;
  categories: CategoryResponse[];
}

/** ADMIN. Closes with the saved service. */
@Component({
  selector: 'app-service-dialog',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    DialogFrameComponent,
    ...FIELD_ERROR,
    ImageFieldComponent,
  ],
  templateUrl: './service-dialog.component.html',
})
export class ServiceDialogComponent {
  private readonly api = inject(CatalogApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  protected readonly data = inject<ServiceDialogData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<SalonServiceResponse>>(DialogRef);
  protected readonly symbol = inject(SessionStore).currencySymbol;
  protected readonly service = this.data.service;
  /** Inactive categories stay selectable only if the service already uses one. */
  protected readonly categories = this.data.categories.filter(
    (c) => c.active || c.id === this.service?.categoryId,
  );

  protected readonly image = signal<File | null>(null);
  protected readonly imageError = signal('');
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    categoryId: this.fb.control<number | null>(
      this.service?.categoryId ?? null,
      Validators.required,
    ),
    name: [this.service?.name ?? '', [Validators.required, Validators.maxLength(120)]],
    description: [this.service?.description ?? '', Validators.maxLength(500)],
    durationMinutes: this.fb.control<number | null>(this.service?.durationMinutes ?? null, [
      Validators.required,
      Validators.min(1),
    ]),
    price: [moneyText(this.service?.price), [Validators.required, moneyValidator(0)]],
    active: [this.service?.active ?? true],
  });

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    const request: SalonServiceRequest = {
      categoryId: v.categoryId!,
      name: v.name.trim(),
      description: v.description.trim() || null,
      durationMinutes: v.durationMinutes!,
      price: parseMoney(v.price) ?? 0,
      active: v.active,
    };
    this.saving.set(true);
    this.error.set('');
    this.imageError.set('');
    const request$ = this.service
      ? this.api.updateService(this.service.id, request)
      : this.api.createService(request, this.image());
    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => this.ref.close(saved),
      error: (e) => {
        const imageError = imageErrorOf(e);
        if (imageError) this.imageError.set(imageError);
        else this.error.set(formErrorBanner(this.form, e));
      },
    });
  }
}
