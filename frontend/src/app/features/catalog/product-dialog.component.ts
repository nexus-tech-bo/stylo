import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Component, ElementRef, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CatalogApi } from '../../core/api/catalog.api';
import { SessionStore } from '../../core/auth/session.store';
import { ProductRequest, ProductResponse } from '../../core/models/api.models';
import {
  formErrorBanner,
  moneyText,
  moneyValidator,
  parseMoney,
} from '../../core/utils/forms.util';
import { rejectInvalid } from '../../shared/utils/forms.util';
import { DialogFrameComponent } from '../../shared/ui/dialog-frame/dialog-frame.component';
import { FIELD_ERROR } from '../../shared/ui/field-error/field-error.component';
import { ImageFieldComponent, imageErrorOf } from './image-field.component';

/** ADMIN. Stock only on create (afterwards: stock adjustment). Closes with the saved product. */
@Component({
  selector: 'app-product-dialog',
  imports: [ReactiveFormsModule, DialogFrameComponent, ...FIELD_ERROR, ImageFieldComponent],
  templateUrl: './product-dialog.component.html',
})
export class ProductDialogComponent {
  private readonly api = inject(CatalogApi);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  protected readonly product = inject<ProductResponse | null>(DIALOG_DATA) ?? null;
  protected readonly ref = inject<DialogRef<ProductResponse>>(DialogRef);
  protected readonly symbol = inject(SessionStore).currencySymbol;

  protected readonly image = signal<File | null>(null);
  protected readonly imageError = signal('');
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    name: [this.product?.name ?? '', [Validators.required, Validators.maxLength(120)]],
    sku: [this.product?.sku ?? '', Validators.maxLength(50)],
    price: [moneyText(this.product?.price), [Validators.required, moneyValidator(0)]],
    stock: this.fb.control<number | null>(
      null,
      this.product ? [] : [Validators.required, Validators.min(0)],
    ),
    minStock: this.fb.control<number | null>(this.product?.minStock ?? null, Validators.min(0)),
    active: [this.product?.active ?? true],
  });

  protected save(): void {
    if (this.saving() || rejectInvalid(this.form, this.host)) return;
    const v = this.form.getRawValue();
    const request: ProductRequest = {
      name: v.name.trim(),
      sku: v.sku.trim() || null,
      price: parseMoney(v.price) ?? 0,
      minStock: v.minStock,
      active: v.active,
    };
    this.saving.set(true);
    this.error.set('');
    this.imageError.set('');
    const request$ = this.product
      ? this.api.updateProduct(this.product.id, request)
      : this.api.createProduct({ ...request, stock: v.stock ?? 0 }, this.image());
    request$.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: (saved) => this.ref.close(saved),
      error: (e) => {
        const imageError = imageErrorOf(e);
        if (imageError) this.imageError.set(imageError);
        else this.error.set(formErrorBanner(this.form, e));
      },
    });
  }
}
