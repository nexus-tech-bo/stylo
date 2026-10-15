import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApi } from '../../core/api/auth.api';
import { SessionStore } from '../../core/auth/session.store';
import { apiError, MSG } from '../../core/http/api-error';
import { homePath, PLATFORM_HOME } from '../../core/navigation/navigation';
import { formErrorBanner } from '../../core/utils/forms.util';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { rejectInvalid } from '../../shared/utils/forms.util';

/** Business login (`/login`) and platform login (`/plataforma/login`, `data.platform`). */
@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink, ...FIELD_ERROR, UiIconComponent],
  templateUrl: './login-page.component.html',
})
export class LoginPageComponent implements OnInit {
  private readonly api = inject(AuthApi);
  private readonly session = inject(SessionStore);
  private readonly router = inject(Router);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;

  readonly platform = input(false);
  readonly returnUrl = input<string>();
  readonly reason = input<string>();

  protected readonly visible = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly info = computed(() =>
    this.reason() === 'expired' && !this.error() ? MSG.expired : '',
  );
  protected readonly form = inject(NonNullableFormBuilder).group({
    businessCode: ['', Validators.required],
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  constructor() {
    // Every input has a placeholder, so :placeholder-shown = "still empty".
    afterNextRender(() =>
      this.host.querySelector<HTMLInputElement>('input:placeholder-shown')?.focus(),
    );
  }

  ngOnInit(): void {
    if (this.reason() === 'suspended') this.error.set(MSG.suspended);
    if (this.platform()) {
      this.form.controls.businessCode.disable();
      return;
    }
    this.form.controls.businessCode.setValue(this.session.lastBusinessCode);
  }

  protected submit(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const { businessCode, username, password } = this.form.getRawValue();
    this.saving.set(true);
    this.error.set('');
    if (this.platform()) {
      this.api
        .platformLogin({ username: username.trim(), password })
        .pipe(finalize(() => this.saving.set(false)))
        .subscribe({
          next: (res) => {
            this.session.setPlatform(res);
            void this.router.navigateByUrl(PLATFORM_HOME);
          },
          error: (e) => this.fail(e, 'Usuario o contraseña incorrectos.'),
        });
      return;
    }
    const code = businessCode.trim().toLowerCase();
    this.api
      .login({ businessCode: code, username: username.trim(), password })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (res) => {
          this.session.lastBusinessCode = code;
          this.session.setBusiness(res);
          const target = this.returnUrl()?.startsWith('/')
            ? this.returnUrl()!
            : homePath(res.user.role);
          void this.router.navigateByUrl(target);
        },
        error: (e) => this.fail(e, 'Código, usuario o contraseña incorrectos.'),
      });
  }

  private fail(error: unknown, credentialsMessage: string): void {
    // Suspended / network / gateway messages come from apiError.
    if (apiError(error).status === 401) this.error.set(credentialsMessage);
    else this.error.set(formErrorBanner(this.form, error));
  }
}
