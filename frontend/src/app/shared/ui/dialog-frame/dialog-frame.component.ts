import { CdkDialogContainer, DialogRef } from '@angular/cdk/dialog';
import { Component, inject, input, output } from '@angular/core';
import { UiIconComponent } from '../icon/ui-icon.component';

let nextId = 0;

/** Glass frame for every dialog: title, close button, scrollable body, `[dialogFooter]` slot. */
@Component({
  selector: 'app-dialog-frame',
  imports: [UiIconComponent],
  template: `
    <header class="mb-5 flex items-start justify-between gap-4">
      <div class="min-w-0">
        <h2 [id]="titleId" class="text-xl font-extrabold text-gray-900">{{ title() }}</h2>
        @if (subtitle()) {
          <p class="mt-1 text-sm text-gray-500">{{ subtitle() }}</p>
        }
      </div>
      <button
        type="button"
        class="btn-icon -mr-2 -mt-1"
        aria-label="Cerrar"
        (click)="closed.emit()"
      >
        <app-ui-icon name="close" class="h-4 w-4" />
      </button>
    </header>
    <div class="-mx-1 min-h-0 flex-1 overflow-y-auto px-1 pb-1"><ng-content /></div>
    <footer class="mt-6 flex flex-wrap justify-end gap-2 empty:hidden">
      <ng-content select="[dialogFooter]" />
    </footer>
  `,
  host: {
    class: 'glass-modal flex max-h-[90vh] w-[calc(100vw-2rem)] max-w-lg flex-col rounded-3xl p-6',
  },
})
export class DialogFrameComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
  readonly closed = output<void>();
  protected readonly titleId = `dialog-title-${nextId++}`;

  constructor() {
    // Same hook MatDialogTitle uses: names the CDK dialog container after our heading.
    const container = inject(DialogRef, { optional: true })?.containerInstance as
      CdkDialogContainer | undefined;
    container?._addAriaLabelledBy(this.titleId);
  }
}
