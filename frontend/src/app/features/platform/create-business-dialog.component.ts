import { DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { PlatformApi } from '../../core/api/platform.api';
import { apiError } from '../../core/http/api-error';
import { CreateBusinessRequest, PlatformBusiness } from '../../core/models/api.models';
import { formErrorBanner } from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';

const slugify = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40);

/** SUPERADMIN. Closes with the created business. */
@Component({
  selector: 'app-create-business-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR, UiIconComponent],
  templateUrl: './create-business-dialog.component.html',
})
export class CreateBusinessDialogComponent {
  private readonly api = inject(PlatformApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  protected readonly ref = inject<DialogRef<PlatformBusiness>>(DialogRef);

  /** The code follows the name until the user edits it. */
  private codeEdited = false;
  protected readonly visible = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    code: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(40),
        Validators.pattern(/^[a-z0-9-]+$/),
      ],
    ],
    taxId: ['', Validators.maxLength(30)],
    phone: ['', Validators.maxLength(30)],
    adminName: ['', [Validators.required, Validators.maxLength(120)]],
    adminUsername: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[A-Za-z0-9._-]+$/),
      ],
    ],
    adminPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
    baseCatalog: [true],
  });

  protected onNameInput(): void {
    if (!this.codeEdited) this.form.controls.code.setValue(slugify(this.form.controls.name.value));
  }

  protected onCodeInput(): void {
    this.codeEdited = true;
  }

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    const request: CreateBusinessRequest = {
      ...v,
      name: v.name.trim(),
      taxId: v.taxId.trim() || null,
      phone: v.phone.trim() || null,
      adminName: v.adminName.trim(),
      adminUsername: v.adminUsername.trim(),
    };
    this.saving.set(true);
    this.error.set('');
    this.api
      .createBusiness(request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (created) => this.ref.close(created),
        error: (e) => {
          if (apiError(e).status === 409) {
            this.form.controls.code.setErrors({ server: 'Este código ya está en uso.' });
            this.form.controls.code.markAsTouched();
          } else {
            this.error.set(formErrorBanner(this.form, e));
          }
        },
      });
  }
}
