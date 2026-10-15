import { Component, DestroyRef, inject, input, model, signal } from '@angular/core';
import { apiError } from '../../core/http/api-error';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';

const MAX_BYTES = 5 * 1024 * 1024;

/** Optional image for create dialogs (≤5 MB, png/jpeg/webp) with preview. */
@Component({
  selector: 'app-image-field',
  imports: [UiIconComponent],
  template: `
    <span class="field-label" id="image-label">Imagen</span>
    @if (preview(); as url) {
      <div class="flex items-center gap-3">
        <img
          [src]="url"
          alt="Vista previa de la imagen"
          class="h-20 w-20 rounded-xl object-cover"
        />
        <button type="button" class="btn btn-ghost btn-sm" (click)="clear()">Quitar imagen</button>
      </div>
    } @else {
      <label
        class="flex cursor-pointer flex-col items-center justify-center gap-1 rounded-2xl border-2 border-dashed border-gray-900/15 bg-white/40 px-4 py-6 text-center text-sm text-gray-500 hover:bg-white/70 has-focus-visible:ring-2 has-focus-visible:ring-primary/70"
      >
        <app-ui-icon name="image" class="h-6 w-6" />
        <span class="font-semibold text-gray-700">Elegir imagen</span>
        <span class="text-xs">PNG, JPG o WEBP · máximo 5 MB</span>
        <input
          type="file"
          class="sr-only"
          accept="image/png,image/jpeg,image/webp"
          aria-labelledby="image-label"
          (change)="pick($event)"
        />
      </label>
    }
    @if (clientError() || error()) {
      <p class="field-error" role="alert">{{ clientError() || error() }}</p>
    }
  `,
  host: { class: 'block' },
})
export class ImageFieldComponent {
  readonly file = model<File | null>(null);
  /** Server-side image error. */
  readonly error = input('');

  protected readonly preview = signal<string | null>(null);
  protected readonly clientError = signal('');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.revoke());
  }

  protected pick(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) return;
    if (file.size > MAX_BYTES) {
      this.clientError.set('La imagen supera 5 MB.');
      return;
    }
    this.clientError.set('');
    this.revoke();
    this.preview.set(URL.createObjectURL(file));
    this.file.set(file);
  }

  protected clear(): void {
    this.revoke();
    this.preview.set(null);
    this.file.set(null);
  }

  private revoke(): void {
    const url = this.preview();
    if (url) URL.revokeObjectURL(url);
  }
}

/** Image problems (413, image/file details, `image` field) go under the image field, not the banner. '' otherwise. */
export function imageErrorOf(error: unknown): string {
  const info = apiError(error);
  if (info.fieldErrors['image']) return info.fieldErrors['image'];
  return info.status === 413 || /image|file/i.test(info.detail) ? info.message : '';
}
