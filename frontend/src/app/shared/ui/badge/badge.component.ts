import { Component, computed, input } from '@angular/core';
import { Tone } from '../../../core/utils/labels.util';

const SOFT: Record<Tone, [string, string]> = {
  success: ['bg-emerald-50 text-emerald-700', 'bg-emerald-500'],
  primary: ['bg-primary/10 text-primary', 'bg-primary'],
  secondary: ['bg-secondary/10 text-rose-700', 'bg-secondary'],
  tertiary: ['bg-tertiary/10 text-orange-800', 'bg-tertiary'],
  neutral: ['bg-gray-900/[0.06] text-gray-600', 'bg-gray-400'],
  danger: ['bg-red-50 text-red-700', 'bg-red-500'],
};

@Component({
  selector: 'app-badge',
  template: `
    <span class="chip" [class]="classes()[0]">
      @if (dot()) {
        <span class="h-1.5 w-1.5 rounded-full" [class]="classes()[1]"></span>
      }
      {{ label() }}
    </span>
  `,
})
export class BadgeComponent {
  readonly label = input.required<string>();
  readonly tone = input<Tone>('neutral');
  readonly dot = input(true);
  protected readonly classes = computed(() => SOFT[this.tone()]);
}
