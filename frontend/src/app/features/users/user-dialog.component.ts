import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { UsersApi } from '../../core/api/users.api';
import { apiError } from '../../core/http/api-error';
import { BusinessRole, UserRequest, UserResponse } from '../../core/models/api.models';
import { formErrorBanner } from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { ROLE_LABELS } from '../../core/utils/labels.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';

export interface UserDialogData {
  user: UserResponse | null;
  selfId: number | null;
}

/** ADMIN. Closes with the saved user. */
@Component({
  selector: 'app-user-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR],
  templateUrl: './user-dialog.component.html',
})
export class UserDialogComponent {
  private readonly api = inject(UsersApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly data = inject<UserDialogData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<UserResponse>>(DialogRef);
  protected readonly user = this.data.user;
  /** Editing yourself: role and active are locked (prevents locking the salon out). */
  protected readonly isSelf = !!this.user && this.user.id === this.data.selfId;
  protected readonly roles: BusinessRole[] = ['ADMIN', 'CASHIER', 'STYLIST'];
  protected readonly roleLabels = ROLE_LABELS;

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    name: [this.user?.name ?? '', [Validators.required, Validators.maxLength(120)]],
    username: [
      this.user?.username ?? '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(/^[A-Za-z0-9._-]+$/),
      ],
    ],
    password: [
      '',
      this.user
        ? [Validators.minLength(6), Validators.maxLength(72)]
        : [Validators.required, Validators.minLength(6), Validators.maxLength(72)],
    ],
    role: this.fb.control<BusinessRole>(this.user?.role ?? 'CASHIER'),
    phone: [this.user?.phone ?? '', Validators.maxLength(30)],
    commissionRate: this.fb.control<number | null>(this.user?.commissionRate ?? null, [
      Validators.min(0),
      Validators.max(100),
    ]),
    active: [this.user?.active ?? true],
  });

  constructor() {
    if (this.isSelf) {
      this.form.controls.role.disable();
      this.form.controls.active.disable();
    }
  }

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    const request: UserRequest = {
      name: v.name.trim(),
      username: v.username.trim(),
      role: v.role,
      phone: v.phone.trim() || null,
      commissionRate: v.commissionRate,
      active: v.active,
      ...(v.password ? { password: v.password } : {}),
    };
    this.saving.set(true);
    this.error.set('');
    const request$ = this.user ? this.api.update(this.user.id, request) : this.api.create(request);
    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => this.ref.close(saved),
      error: (e) => {
        if (apiError(e).status === 409) {
          this.form.controls.username.setErrors({ server: 'Este usuario ya existe.' });
          this.form.controls.username.markAsTouched();
        } else {
          this.error.set(formErrorBanner(this.form, e));
        }
      },
    });
  }
}
