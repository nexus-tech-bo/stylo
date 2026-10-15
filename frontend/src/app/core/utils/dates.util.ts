/** Local-date helpers. Never use `toISOString()` / `new Date('yyyy-MM-dd')` for LocalDate: both are UTC and shift a day. */

export interface DateRange {
  from: string;
  to: string;
}

export type PresetKey = 'today' | 'yesterday' | 'last7' | 'thisMonth' | 'lastMonth';

const pad = (n: number) => String(n).padStart(2, '0');

/** Date → local 'yyyy-MM-dd'. */
export function toLocalDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** 'yyyy-MM-dd' → local Date at midnight. */
export function parseLocalDate(value: string): Date {
  const [y, m, d] = value.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function today(): string {
  return toLocalDate(new Date());
}

const shift = (days: number) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return toLocalDate(d);
};

export const PRESET_LABELS: Record<PresetKey, string> = {
  today: 'Hoy',
  yesterday: 'Ayer',
  last7: '7 días',
  thisMonth: 'Este mes',
  lastMonth: 'Mes anterior',
};

export function presetRange(key: PresetKey): DateRange {
  const now = new Date();
  switch (key) {
    case 'today':
      return { from: today(), to: today() };
    case 'yesterday':
      return { from: shift(-1), to: shift(-1) };
    case 'last7':
      return { from: shift(-6), to: today() };
    case 'thisMonth':
      return { from: toLocalDate(new Date(now.getFullYear(), now.getMonth(), 1)), to: today() };
    case 'lastMonth':
      return {
        from: toLocalDate(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
        to: toLocalDate(new Date(now.getFullYear(), now.getMonth(), 0)),
      };
  }
}

/** Client-side range check before requesting (backend max = 1 year). '' when valid. */
export function rangeError(range: DateRange, maxOneYear = false): string {
  if (!range.from || !range.to) return 'Selecciona ambas fechas.';
  if (range.from > range.to) return 'La fecha inicial no puede ser posterior a la final.';
  if (maxOneYear) {
    const limit = parseLocalDate(range.from);
    limit.setFullYear(limit.getFullYear() + 1);
    if (parseLocalDate(range.to) > limit) return 'El rango máximo es de 1 año.';
  }
  return '';
}
