import { AbstractControl, FormGroup } from '@angular/forms';

/** Spanish message for the first client/server error of a touched control. */
export function controlError(control: AbstractControl | null | undefined): string | null {
  if (!control?.errors || !control.touched) return null;
  const e = control.errors;
  if (e['server']) return e['server'];
  if (e['required']) return 'Este campo es obligatorio.';
  if (e['email']) return 'Ingresa un email válido.';
  if (e['maxlength']) return `Máximo ${e['maxlength'].requiredLength} caracteres.`;
  if (e['minlength']) return `Mínimo ${e['minlength'].requiredLength} caracteres.`;
  if (e['min']) return `Debe ser mayor o igual a ${e['min'].min}.`;
  if (e['max']) return `Debe ser menor o igual a ${e['max'].max}.`;
  if (e['pattern']) return 'El formato no es válido.';
  const custom = Object.values(e).find((v) => typeof v === 'string');
  return (custom as string | undefined) ?? 'Valor no válido.';
}

/** Marks everything touched and focuses the first invalid control inside `host`. Returns true when the form is invalid. */
export function rejectInvalid(form: FormGroup, host: HTMLElement): boolean {
  if (form.valid) return false;
  form.markAllAsTouched();
  setTimeout(() =>
    host
      .querySelector<HTMLElement>('.ng-invalid[formControlName], .ng-invalid[formcontrolname]')
      ?.focus(),
  );
  return true;
}
