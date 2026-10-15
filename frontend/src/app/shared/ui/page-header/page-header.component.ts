import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  template: `
    <div class="min-w-0">
      <h1 class="text-[28px] font-extrabold tracking-tight text-gray-900 lg:text-[34px]">
        {{ title() }}
      </h1>
      @if (subtitle()) {
        <p class="text-[15px] font-medium text-gray-500">{{ subtitle() }}</p>
      }
    </div>
    <div class="flex flex-wrap items-center gap-2"><ng-content /></div>
  `,
  host: { class: 'mb-6 flex flex-wrap items-center justify-between gap-4' },
})
export class PageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
