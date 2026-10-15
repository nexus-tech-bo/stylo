import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CatalogApi } from '../../core/api/catalog.api';
import { CategoryResponse } from '../../core/models/api.models';
import { formErrorBanner } from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';

/** ADMIN. Closes with the saved category. */
@Component({
  selector: 'app-category-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR],
  template: `
    <app-dialog-frame
      [title]="category ? 'Editar categoría' : 'Nueva categoría'"
      (closed)="ref.close()"
    >
      <form id="category-form" [formGroup]="form" (ngSubmit)="save()" class="space-y-4" novalidate>
        @if (error()) {
          <p class="form-banner" role="alert">{{ error() }}</p>
        }
        <div>
          <label class="field-label" for="category-name">Nombre *</label>
          <input
            id="category-name"
            cdkFocusInitial
            class="field-input"
            formControlName="name"
            maxlength="80"
            autocomplete="off"
          />
          <app-field-error [control]="form.controls.name" />
        </div>
        <label class="flex cursor-pointer items-center gap-2 text-sm font-semibold text-gray-600">
          <input type="checkbox" class="h-4 w-4 accent-primary" formControlName="active" /> Activa
        </label>
      </form>
      <div dialogFooter class="contents">
        <button type="button" class="btn btn-secondary" (click)="ref.close()">Cancelar</button>
        <button type="submit" form="category-form" class="btn btn-primary" [disabled]="saving()">
          {{ saving() ? 'Guardando…' : 'Guardar categoría' }}
        </button>
      </div>
    </app-dialog-frame>
  `,
})
export class CategoryDialogComponent {
  private readonly api = inject(CatalogApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  protected readonly category = inject<CategoryResponse | null>(DIALOG_DATA) ?? null;
  protected readonly ref = inject<DialogRef<CategoryResponse>>(DialogRef);

  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly form = inject(NonNullableFormBuilder).group({
    name: [this.category?.name ?? '', [Validators.required, Validators.maxLength(80)]],
    active: [this.category?.active ?? true],
  });

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    const request = { name: v.name.trim(), active: v.active };
    this.saving.set(true);
    this.error.set('');
    const request$ = this.category
      ? this.api.updateCategory(this.category.id, request)
      : this.api.createCategory(request);
    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => this.ref.close(saved),
      error: (e) => this.error.set(formErrorBanner(this.form, e)),
    });
  }
}
