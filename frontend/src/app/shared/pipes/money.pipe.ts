import { inject, Pipe, PipeTransform } from '@angular/core';
import { SessionStore } from '../../core/auth/session.store';

// 'de-DE' = same separators as 'es' (1.234,50) but also groups 4-digit numbers ('es' prints 1234,50).
export const MONEY_FORMAT = new Intl.NumberFormat('de-DE', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** `{{ value | money }}` → 'Bs 1.234,50' / '−Bs 10,00'. `{{ value | money: false }}` omits the symbol. */
@Pipe({ name: 'money', pure: false })
export class MoneyPipe implements PipeTransform {
  private readonly session = inject(SessionStore);

  transform(value: number | null | undefined, withSymbol = true): string {
    if (value === null || value === undefined) return '—';
    const symbol = withSymbol ? this.session.currencySymbol() : '';
    const text = `${symbol ? symbol + ' ' : ''}${MONEY_FORMAT.format(Math.abs(value))}`;
    return value < 0 ? `−${text}` : text;
  }
}
