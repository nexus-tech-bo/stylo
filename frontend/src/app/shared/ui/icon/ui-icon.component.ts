import { Component, computed, input } from '@angular/core';

const ICONS: Record<string, string[]> = {
  home: ['M3 10.5 12 3l9 7.5', 'M5 9.5V21h14V9.5', 'M9.5 21v-6h5v6'],
  cart: [
    'M3 4h2l2.4 11.2a1 1 0 0 0 1 .8h9.2a1 1 0 0 0 1-.8L20 8H6.2',
    'M9.5 20a1 1 0 1 1-2 0 1 1 0 0 1 2 0',
    'M18.5 20a1 1 0 1 1-2 0 1 1 0 0 1 2 0',
  ],
  receipt: ['M6 3h12v18l-3-2-3 2-3-2-3 2V3z', 'M9 8h6', 'M9 12h6', 'M9 16h3'],
  cash: ['M3 6h18v12H3z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z', 'M6 9v.01', 'M18 15v.01'],
  users: [
    'M16 20v-1.5A3.5 3.5 0 0 0 12.5 15h-5A3.5 3.5 0 0 0 4 18.5V20',
    'M10 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z',
    'M20 20v-1.5a3.5 3.5 0 0 0-2.5-3.35',
    'M15.5 4.15a3.5 3.5 0 0 1 0 6.7',
  ],
  scissors: [
    'M6 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
    'M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
    'M20 4 8.1 15.9',
    'M14.5 14.5 20 20',
    'M8.1 8.1 12 12',
  ],
  chart: ['M3 21h18', 'M6 17v-5', 'M11 17V6', 'M16 17V9', 'M21 17v-3'],
  percent: [
    'M19 5 5 19',
    'M7 9a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
    'M17 19a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
  ],
  staff: [
    'M4 5h16v14H4z',
    'M9 12a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
    'M6.5 16a3 3 0 0 1 5 0',
    'M14 9.5h3.5',
    'M14 13h3.5',
  ],
  settings: [
    'M4 6h9',
    'M17 6h3',
    'M4 12h3',
    'M11 12h9',
    'M4 18h11',
    'M19 18h1',
    'M15 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
    'M9 14a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
    'M17 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4z',
  ],
  logout: ['M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3', 'M10 16l-4-4 4-4', 'M6 12h10'],
  key: ['M7 15a4 4 0 1 1 3.87-5H21v3h-2v3h-3v-3h-5.13A4 4 0 0 1 7 15z'],
  menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
  close: ['M6 6l12 12', 'M18 6 6 18'],
  plus: ['M12 5v14', 'M5 12h14'],
  minus: ['M5 12h14'],
  search: ['M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z', 'M20 20l-4-4'],
  edit: ['M4 20h4L19 9l-4-4L4 16v4z', 'M13.5 6.5l4 4'],
  trash: ['M4 7h16', 'M10 11v6', 'M14 11v6', 'M6 7l1 13h10l1-13', 'M9 7V4h6v3'],
  chevronLeft: ['M15 6l-6 6 6 6'],
  chevronRight: ['M9 6l6 6-6 6'],
  chevronDown: ['M6 9l6 6 6-6'],
  arrowLeft: ['M19 12H5', 'M11 18l-6-6 6-6'],
  check: ['M5 12.5l4.5 4.5L19 7'],
  alert: [
    'M12 9v4',
    'M12 17h.01',
    'M10.3 4.3 2.6 18a2 2 0 0 0 1.7 3h15.4a2 2 0 0 0 1.7-3L13.7 4.3a2 2 0 0 0-3.4 0z',
  ],
  info: ['M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z', 'M12 11v5', 'M12 8h.01'],
  lock: ['M6 11h12v10H6z', 'M8.5 11V7.5a3.5 3.5 0 0 1 7 0V11'],
  eye: ['M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z', 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z'],
  eyeOff: [
    'M3 3l18 18',
    'M10.6 6.1A10.7 10.7 0 0 1 12 6c6.5 0 10 6 10 6a17 17 0 0 1-3.2 3.9',
    'M6.6 6.6A17 17 0 0 0 2 12s3.5 7 10 7a9.8 9.8 0 0 0 5.4-1.6',
    'M9.9 9.9a3 3 0 0 0 4.2 4.2',
  ],
  download: ['M12 4v12', 'M7 11l5 5 5-5', 'M5 20h14'],
  box: ['M3 7l9-4 9 4-9 4-9-4z', 'M3 7v10l9 4 9-4V7', 'M12 11v10'],
  building: [
    'M4 21V5l8-2v18',
    'M12 9h8v12',
    'M8 8h.01',
    'M8 12h.01',
    'M8 16h.01',
    'M16 13h.01',
    'M16 17h.01',
    'M2 21h20',
  ],
  pause: ['M9 5v14', 'M15 5v14'],
  play: ['M7 4l13 8-13 8V4z'],
  image: [
    'M4 5h16v14H4z',
    'M4 16l4.5-4.5 4 4L15 13l5 5',
    'M15.5 10a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z',
  ],
  table: ['M3 5h18v14H3z', 'M3 10h18', 'M9 10v9'],
};

/** Inline stroke icon (24×24, currentColor). Size with h-/w- classes on the host. */
@Component({
  selector: 'app-ui-icon',
  template: `
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      [attr.stroke-width]="strokeWidth()"
      stroke-linecap="round"
      stroke-linejoin="round"
      class="h-full w-full"
      aria-hidden="true"
    >
      @for (d of paths(); track $index) {
        <path [attr.d]="d" />
      }
    </svg>
  `,
  host: { class: 'inline-block shrink-0' },
})
export class UiIconComponent {
  readonly name = input.required<string>();
  readonly strokeWidth = input(1.9);
  protected readonly paths = computed(() => ICONS[this.name()] ?? []);
}
