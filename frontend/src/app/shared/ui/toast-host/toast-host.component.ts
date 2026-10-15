import { Component, inject } from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { UiIconComponent } from '../icon/ui-icon.component';

@Component({
  selector: 'app-toast-host',
  imports: [UiIconComponent],
  template: `
    @for (toast of notifications.toasts(); track toast.id) {
      <div
        class="glass-modal flex items-start gap-3 rounded-2xl p-4"
        [attr.role]="toast.kind === 'error' ? 'alert' : 'status'"
      >
        <span
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full"
          [class]="
            toast.kind === 'error' ? 'bg-red-100 text-red-600' : 'bg-emerald-100 text-emerald-600'
          "
        >
          <app-ui-icon
            [name]="toast.kind === 'error' ? 'alert' : 'check'"
            class="h-4 w-4"
            [strokeWidth]="2.4"
          />
        </span>
        <p class="min-w-0 flex-1 pt-1 text-sm font-semibold text-gray-800">{{ toast.message }}</p>
        <button
          type="button"
          class="btn-icon h-8 w-8"
          aria-label="Cerrar notificación"
          (click)="notifications.dismiss(toast.id)"
        >
          <app-ui-icon name="close" class="h-4 w-4" />
        </button>
      </div>
    }
  `,
  host: {
    class:
      'pointer-events-none fixed inset-x-4 bottom-4 z-[1100] flex flex-col gap-2 sm:inset-x-auto sm:top-4 sm:right-4 sm:bottom-auto sm:w-96 [&>*]:pointer-events-auto',
  },
})
export class ToastHostComponent {
  protected readonly notifications = inject(NotificationService);
}
