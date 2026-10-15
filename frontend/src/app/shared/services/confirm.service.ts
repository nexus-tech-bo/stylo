import { Dialog, DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { DialogFrameComponent } from '../ui/dialog-frame/dialog-frame.component';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [DialogFrameComponent],
  template: `
    <app-dialog-frame [title]="data.title" (closed)="ref.close(false)">
      <p class="text-sm text-gray-700">{{ data.message }}</p>
      <div dialogFooter class="contents">
        <button type="button" class="btn btn-secondary" cdkFocusInitial (click)="ref.close(false)">
          Cancelar
        </button>
        <button
          type="button"
          class="btn"
          [class]="data.danger ? 'btn-danger' : 'btn-primary'"
          (click)="ref.close(true)"
        >
          {{ data.confirmLabel ?? 'Confirmar' }}
        </button>
      </div>
    </app-dialog-frame>
  `,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmOptions>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly dialog = inject(Dialog);

  /** Emits once: true when confirmed, false otherwise. */
  ask(options: ConfirmOptions): Observable<boolean> {
    return this.dialog
      .open<boolean>(ConfirmDialogComponent, { data: options })
      .closed.pipe(map(Boolean));
  }
}
