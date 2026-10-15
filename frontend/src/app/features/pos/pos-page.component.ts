import { Dialog } from '@angular/cdk/dialog';
import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { filter, finalize } from 'rxjs';
import { CashRegisterApi } from '../../core/api/cash-register.api';
import { CatalogApi } from '../../core/api/catalog.api';
import { SalesApi } from '../../core/api/sales.api';
import { UsersApi } from '../../core/api/users.api';
import { SessionStore } from '../../core/auth/session.store';
import { apiError, isNoOpenRegister } from '../../core/http/api-error';
import {
  CustomerResponse,
  ItemType,
  PaymentMethod,
  ProductResponse,
  SaleRequest,
  SalonServiceResponse,
} from '../../core/models/api.models';
import { moneyText, parseMoney, roundMoney } from '../../core/utils/forms.util';
import { PAYMENT_LABELS, PAYMENT_METHODS } from '../../core/utils/labels.util';
import { CustomerPickerComponent } from '../../shared/components/customer-picker/customer-picker.component';
import { OpenRegisterDialogComponent } from '../../shared/dialogs/open-register-dialog/open-register-dialog.component';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { NotificationService } from '../../shared/services/notification.service';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import { SegmentedTabsComponent } from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import {
  addProduct,
  addService,
  CartLine,
  cartSubtotal,
  checkoutError,
  discountError,
  includedTax,
  maxQuantity,
  receivedError,
  setQuantity,
  suggestedBills,
  validDiscount,
} from './cart';
import { SaleResultDialogComponent } from './sale-result-dialog.component';

@Component({
  selector: 'app-pos-page',
  imports: [
    RouterLink,
    MoneyPipe,
    BadgeComponent,
    CustomerPickerComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    SegmentedTabsComponent,
    UiIconComponent,
  ],
  templateUrl: './pos-page.component.html',
})
export class PosPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly session = inject(SessionStore);
  private readonly registerApi = inject(CashRegisterApi);
  private readonly catalogApi = inject(CatalogApi);
  private readonly usersApi = inject(UsersApi);
  private readonly salesApi = inject(SalesApi);
  private readonly cartSection = viewChild<ElementRef<HTMLElement>>('cart');

  protected readonly isAdmin = this.session.isAdmin;
  protected readonly paymentLabels = PAYMENT_LABELS;
  protected readonly paymentMethods = PAYMENT_METHODS;
  protected readonly maxQuantity = maxQuantity;
  protected readonly tabs = [
    { id: 'SERVICE', label: 'Servicios' },
    { id: 'PRODUCT', label: 'Productos' },
  ];

  // ── Server state ──
  protected readonly register = rxResource({ stream: () => this.registerApi.current() });
  protected readonly services = rxResource({ stream: () => this.catalogApi.services(true) });
  protected readonly products = rxResource({ stream: () => this.catalogApi.products(true) });
  protected readonly categories = rxResource({ stream: () => this.catalogApi.categories() });
  protected readonly stylists = rxResource({ stream: () => this.usersApi.stylists() });
  protected readonly stylistList = computed(() =>
    this.stylists.hasValue() ? this.stylists.value() : [],
  );

  // ── Catalog filters ──
  protected readonly tab = signal<ItemType>('SERVICE');
  protected readonly search = signal('');
  protected readonly categoryId = signal<number | null>(null);
  protected readonly catalogError = computed(
    () => !!(this.tab() === 'SERVICE' ? this.services.error() : this.products.error()),
  );
  protected readonly catalogLoading = computed(
    () => !(this.tab() === 'SERVICE' ? this.services.hasValue() : this.products.hasValue()),
  );
  protected readonly activeCategories = computed(() =>
    this.categories.hasValue() ? this.categories.value().filter((c) => c.active) : [],
  );
  protected readonly visibleServices = computed(() => {
    if (!this.services.hasValue()) return [];
    const q = this.search().trim().toLowerCase();
    const cat = this.categoryId();
    return this.services
      .value()
      .filter((s) => (!cat || s.categoryId === cat) && (!q || s.name.toLowerCase().includes(q)));
  });
  protected readonly visibleProducts = computed(() => {
    if (!this.products.hasValue()) return [];
    const q = this.search().trim().toLowerCase();
    return this.products
      .value()
      .filter((p) => !q || p.name.toLowerCase().includes(q) || p.sku?.toLowerCase().includes(q));
  });

  // ── Cart ──
  private nextKey = 0;
  protected readonly lines = signal<CartLine[]>([]);
  protected readonly customer = signal<CustomerResponse | null>(null);
  protected readonly discountText = signal('');
  protected readonly method = signal<PaymentMethod>('CASH');
  protected readonly receivedText = signal('');
  protected readonly notes = signal('');
  protected readonly showNotes = signal(false);
  protected readonly saving = signal(false);
  protected readonly saleError = signal('');

  protected readonly itemCount = computed(() => this.lines().reduce((n, l) => n + l.quantity, 0));
  protected readonly subtotal = computed(() => cartSubtotal(this.lines()));
  protected readonly discount = computed(() => validDiscount(this.discountText(), this.subtotal()));
  protected readonly discountError = computed(() =>
    discountError(this.discountText(), this.subtotal()),
  );
  /** Tax is included in prices: total = subtotal − discount (preview; the response is the truth). */
  protected readonly total = computed(() => roundMoney(this.subtotal() - this.discount()));
  protected readonly taxRate = computed(() => this.session.settings()?.taxRate ?? 0);
  protected readonly tax = computed(() => includedTax(this.total(), this.taxRate()));
  protected readonly received = computed(() => parseMoney(this.receivedText()));
  protected readonly receivedError = computed(() =>
    this.method() === 'CASH' ? receivedError(this.receivedText(), this.total()) : '',
  );
  protected readonly change = computed(() =>
    roundMoney(Math.max(0, (this.received() ?? 0) - this.total())),
  );
  protected readonly bills = computed(() => suggestedBills(this.total()));
  /** Why "Cobrar" is disabled ('' = can charge). */
  protected readonly blockReason = computed(() =>
    checkoutError({
      lineCount: this.lines().length,
      subtotal: this.subtotal(),
      discountText: this.discountText(),
      method: this.method(),
      receivedText: this.receivedText(),
    }),
  );

  protected selectTab(id: string): void {
    this.tab.set(id as ItemType);
    this.search.set('');
  }

  protected reloadCatalog(): void {
    if (this.tab() === 'SERVICE') this.services.reload();
    else this.products.reload();
  }

  protected addService(service: SalonServiceResponse): void {
    const stylists = this.stylistList();
    this.lines.update((lines) =>
      addService(lines, service, this.nextKey++, stylists.length === 1 ? stylists[0].id : null),
    );
  }

  protected addProduct(product: ProductResponse): void {
    this.lines.update((lines) => addProduct(lines, product, this.nextKey++));
  }

  protected setQuantity(line: CartLine, quantity: number): void {
    this.lines.update((lines) => setQuantity(lines, line.key, quantity));
  }

  protected setStylist(line: CartLine, value: string): void {
    const stylistId = value ? Number(value) : null;
    this.lines.update((lines) => lines.map((l) => (l.key === line.key ? { ...l, stylistId } : l)));
  }

  protected remove(line: CartLine): void {
    this.lines.update((lines) => lines.filter((l) => l.key !== line.key));
  }

  protected clear(): void {
    this.lines.set([]);
    this.customer.set(null);
    this.discountText.set('');
    this.method.set('CASH');
    this.receivedText.set('');
    this.notes.set('');
    this.showNotes.set(false);
    this.saleError.set('');
  }

  protected setReceived(amount: number): void {
    this.receivedText.set(moneyText(amount));
  }

  protected goToCart(): void {
    const section = this.cartSection()?.nativeElement;
    section?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    section?.focus({ preventScroll: true });
  }

  protected openRegister(): void {
    this.dialog
      .open(OpenRegisterDialogComponent)
      .closed.pipe(filter(Boolean))
      .subscribe(() => this.register.reload());
  }

  protected charge(): void {
    if (this.blockReason() || this.saving()) return;
    const request: SaleRequest = {
      customerId: this.customer()?.id ?? null,
      items: this.lines().map((l) => ({
        type: l.type,
        itemId: l.itemId,
        quantity: l.quantity,
        stylistId: l.stylistId,
      })),
      discount: this.discount(),
      paymentMethod: this.method(),
      amountReceived: this.method() === 'CASH' ? (this.received() ?? undefined) : undefined,
      notes: this.notes().trim() || null,
    };
    this.saving.set(true);
    this.saleError.set('');
    this.salesApi
      .create(request)
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (sale) => {
          this.notifications.success(`Venta #${sale.id} registrada`);
          this.clear();
          this.products.reload(); // stock changed
          this.dialog
            .open<'detail'>(SaleResultDialogComponent, { data: sale })
            .closed.pipe(filter((r) => r === 'detail'))
            .subscribe(() => void this.router.navigate(['/ventas', sale.id]));
        },
        error: (e) => {
          const info = apiError(
            e,
            'No se pudo registrar la venta. Revisa el stock disponible y el monto recibido.',
          );
          const fields = Object.values(info.fieldErrors);
          this.saleError.set(fields.length ? fields.join(' ') : info.message);
          if (isNoOpenRegister(e)) this.register.reload();
        },
      });
  }
}
