import { Component, input } from '@angular/core';
import { UiIconComponent } from '../icon/ui-icon.component';

@Component({
  selector: 'app-empty-state',
  imports: [UiIconComponent],
  template: `
    <span
      class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-white/60 text-gray-500"
    >
      <app-ui-icon [name]="icon()" class="h-6 w-6" />
    </span>
    <p class="font-bold text-gray-900">{{ title() }}</p>
    @if (message()) {
      <p class="mt-1 text-sm text-gray-500">{{ message() }}</p>
    }
    <div class="mt-4 flex flex-wrap justify-center gap-2 empty:hidden"><ng-content /></div>
  `,
  host: {
    class: 'block rounded-2xl border-2 border-dashed border-gray-900/10 px-6 py-10 text-center',
  },
})
export class EmptyStateComponent {
  readonly icon = input('info');
  readonly title = input.required<string>();
  readonly message = input<string>();
}
