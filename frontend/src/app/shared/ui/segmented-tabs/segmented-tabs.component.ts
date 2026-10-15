import { Component, input, output } from '@angular/core';

export interface TabItem {
  id: string;
  label: string;
}

@Component({
  selector: 'app-segmented-tabs',
  template: `
    @for (item of items(); track item.id) {
      <button
        type="button"
        role="tab"
        class="cursor-pointer rounded-xl px-4 py-2 text-sm font-semibold whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:ring-primary/70 focus-visible:outline-none"
        [class]="
          item.id === selectedId()
            ? 'bg-white/95 text-primary shadow-md'
            : 'text-gray-500 hover:text-gray-700'
        "
        [attr.aria-selected]="item.id === selectedId()"
        (click)="select.emit(item.id)"
      >
        {{ item.label }}
      </button>
    }
  `,
  host: {
    class:
      'glass-panel inline-flex max-w-full items-center gap-1 overflow-x-auto rounded-2xl p-1.5',
    role: 'tablist',
  },
})
export class SegmentedTabsComponent {
  readonly items = input.required<TabItem[]>();
  readonly selectedId = input.required<string>();
  readonly select = output<string>();
}
