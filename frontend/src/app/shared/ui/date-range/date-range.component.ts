import { Component, input, output } from '@angular/core';
import { DateRange, PRESET_LABELS, presetRange, PresetKey } from '../../../core/utils/dates.util';

let nextId = 0;

/** Two native date inputs + preset pills. Emits the whole range on any change. */
@Component({
  selector: 'app-date-range',
  template: `
    <div class="flex flex-wrap items-end gap-3">
      <div>
        <label class="field-label" [for]="id + '-from'">Desde</label>
        <input
          [id]="id + '-from'"
          type="date"
          class="field-input py-2"
          [value]="from()"
          (change)="emit($any($event.target).value, to())"
        />
      </div>
      <div>
        <label class="field-label" [for]="id + '-to'">Hasta</label>
        <input
          [id]="id + '-to'"
          type="date"
          class="field-input py-2"
          [value]="to()"
          (change)="emit(from(), $any($event.target).value)"
        />
      </div>
      <div class="flex flex-wrap gap-1.5 pb-1" role="group" aria-label="Periodos rápidos">
        @for (key of presets(); track key) {
          <button
            type="button"
            class="pill"
            [attr.aria-pressed]="isActive(key)"
            (click)="rangeChange.emit(range(key))"
          >
            {{ labels[key] }}
          </button>
        }
      </div>
    </div>
    @if (error()) {
      <p class="field-error" role="alert">{{ error() }}</p>
    }
  `,
  host: { class: 'block' },
})
export class DateRangeComponent {
  readonly from = input.required<string>();
  readonly to = input.required<string>();
  readonly presets = input<PresetKey[]>(['today', 'last7', 'thisMonth', 'lastMonth']);
  readonly error = input('');
  readonly rangeChange = output<DateRange>();

  protected readonly id = `range-${nextId++}`;
  protected readonly labels = PRESET_LABELS;
  protected readonly range = presetRange;

  protected isActive(key: PresetKey): boolean {
    const r = presetRange(key);
    return r.from === this.from() && r.to === this.to();
  }

  protected emit(from: string, to: string): void {
    this.rangeChange.emit({ from, to });
  }
}
