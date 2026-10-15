import { DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { finalize } from 'rxjs';
import { UsersApi } from '../../../core/api/users.api';
import { formErrorBanner } from '../../../core/utils/forms.util';
import { rejectInvalid } from '../../utils/forms.util';
import { NotificationService } from '../../services/notification.service';
import { DialogFrameComponent } from '../../ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../ui/field-error/field-error.component';
import { UiIconComponent } from '../../ui/icon/ui-icon.component';

const matchesPassword: ValidatorFn = (control) =>
  control.value && control.value !== control.parent?.get('password')?.value
    ? { mismatch: 'Las contraseñas no coinciden.' }
    : null;

/** Any business role. Closes with true when changed. */
@Component({
  selector: 'app-change-password-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR, UiIconComponent],
  templateUrl: './change-password-dialog.component.html',
})
export class ChangePasswordDialogComponent {
  private readonly api = inject(UsersApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly notifications = inject(NotificationService);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);

  protected readonly visible = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(72)]],
    confirm: ['', [Validators.required, matchesPassword]],
  });

  constructor() {
    // Re-check the confirmation when the first field changes.
    this.form.controls.password.valueChanges.subscribe(() =>
      this.form.controls.confirm.updateValueAndValidity({ emitEvent: false }),
    );
  }

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    this.saving.set(true);
    this.error.set('');
    this.api
      .changeMyPassword(this.form.getRawValue().password)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.notifications.success('Contraseña actualizada');
          this.ref.close(true);
        },
        error: (e) => this.error.set(formErrorBanner(this.form, e)),
      });
  }
}
