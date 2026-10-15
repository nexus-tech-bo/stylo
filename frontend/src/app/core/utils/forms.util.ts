import { FormGroup, ValidatorFn } from '@angular/forms';
import { apiError } from '../http/api-error';

/** Accepts '1234.5', '1234,5' and '1.234,50'. null when not a number. */
export function parseMoney(value: string | number | null | undefined): number | null {
  if (value === null || value === undefined) return null;
  if (typeof value === 'number') return Number.isFinite(value) ? value : null;
  let text = value.replace(/\s/g, '');
  if (!text) return null;
  if (text.includes(',')) text = text.replace(/\./g, '').replace(',', '.');
  if (!/^-?\d+(\.\d+)?$/.test(text)) return null;
  return Number(text);
}

/** Number → editable text with comma decimals. */
export function moneyText(value: number | null | undefined): string {
  return value === null || value === undefined ? '' : String(value).replace('.', ',');
}

/** Rounds to cents (avoids float drift in previews). */
export const roundMoney = (value: number) => Math.round(value * 100) / 100;

/** Text money control: empty passes (combine with `required`), otherwise must parse and be ≥ min. */
export function moneyValidator(min = 0): ValidatorFn {
  return (control) => {
    if (control.value === null || control.value === '') return null;
    const n = parseMoney(control.value);
    if (n === null) return { money: 'Ingresa un monto válido.' };
    return n < min ? { min: { min, actual: n } } : null;
  };
}

/**
 * Applies 400 field errors to matching controls (`server` error, cleared on next edit) and
 * returns the banner message ('' when every error landed on a control).
 */
export function formErrorBanner(form: FormGroup, error: unknown, ruleFallback?: string): string {
  const info = apiError(error, ruleFallback);
  const unmapped: string[] = [];
  for (const [field, message] of Object.entries(info.fieldErrors)) {
    const control = form.get(field);
    if (control) {
      control.setErrors({ server: message });
      control.markAsTouched();
    } else {
      unmapped.push(message);
    }
  }
  if (Object.keys(info.fieldErrors).length) return unmapped.join(' ');
  return info.message;
}
