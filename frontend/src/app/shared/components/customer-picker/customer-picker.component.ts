import { Dialog } from '@angular/cdk/dialog';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  model,
  signal,
  viewChild,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { filter, map, switchMap, timer } from 'rxjs';
import { CustomersApi } from '../../../core/api/customers.api';
import { CustomerResponse } from '../../../core/models/api.models';
import { CustomerDialogComponent } from '../../dialogs/customer-dialog/customer-dialog.component';
import { UiIconComponent } from '../../ui/icon/ui-icon.component';

let nextId = 0;

/** Searchable customer dropdown (null = "Consumidor final"), with "+ Nuevo cliente". */
@Component({
  selector: 'app-customer-picker',
  imports: [UiIconComponent],
  templateUrl: './customer-picker.component.html',
  host: { class: 'relative block', '(keydown.escape)': 'close()' },
})
export class CustomerPickerComponent {
  private readonly api = inject(CustomersApi);
  private readonly dialog = inject(Dialog);
  private readonly injector = inject(Injector);
  private readonly trigger = viewChild<ElementRef<HTMLButtonElement>>('trigger');
  private readonly search = viewChild<ElementRef<HTMLInputElement>>('search');

  readonly customer = model<CustomerResponse | null>(null);

  protected readonly id = `customer-picker-${nextId++}`;
  protected readonly open = signal(false);
  protected readonly query = signal('');
  protected readonly active = signal(0);

  /** Debounced by the 300 ms timer: a new query cancels the pending one. */
  protected readonly results = rxResource({
    params: () => (this.open() ? this.query().trim() : undefined),
    stream: ({ params }) =>
      timer(300).pipe(
        switchMap(() => this.api.list({ q: params, size: 8 })),
        map((page) => page.content),
      ),
  });
  protected readonly options = computed(() =>
    this.results.hasValue() ? (this.results.value() ?? []) : [],
  );
  /** Options + the trailing "Nuevo cliente" row. */
  protected readonly optionCount = computed(() => this.options().length + 1);

  protected toggle(): void {
    if (this.open()) return this.close();
    this.query.set('');
    this.active.set(0);
    this.open.set(true);
    afterNextRender(() => this.search()?.nativeElement.focus(), { injector: this.injector });
  }

  protected close(): void {
    if (!this.open()) return;
    this.open.set(false);
    this.trigger()?.nativeElement.focus();
  }

  protected onSearch(value: string): void {
    this.query.set(value);
    this.active.set(0);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const count = this.optionCount();
    if (event.key === 'ArrowDown') this.active.update((i) => (i + 1) % count);
    else if (event.key === 'ArrowUp') this.active.update((i) => (i - 1 + count) % count);
    else if (event.key === 'Enter') {
      const option = this.options()[this.active()];
      if (option) this.choose(option);
      else this.createNew();
    } else return;
    event.preventDefault();
  }

  protected choose(customer: CustomerResponse | null): void {
    this.customer.set(customer);
    this.close();
  }

  protected createNew(): void {
    this.close();
    this.dialog
      .open<CustomerResponse>(CustomerDialogComponent, { data: null })
      .closed.pipe(filter(Boolean))
      .subscribe((created) => this.customer.set(created));
  }
}
