import { Dialog } from '@angular/cdk/dialog';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { filter, switchMap } from 'rxjs';
import { CatalogApi } from '../../core/api/catalog.api';
import { SessionStore } from '../../core/auth/session.store';
import { apiErrorMessage } from '../../core/http/api-error';
import {
  CategoryResponse,
  ProductResponse,
  SalonServiceResponse,
} from '../../core/models/api.models';
import { activeLabel, initials, stockLabel } from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { ConfirmService } from '../../shared/services/confirm.service';
import { NotificationService } from '../../shared/services/notification.service';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state/empty-state.component';
import { UiIconComponent } from '../../shared/ui/icon/ui-icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';
import {
  SegmentedTabsComponent,
  TabItem,
} from '../../shared/ui/segmented-tabs/segmented-tabs.component';
import { CategoryDialogComponent } from './category-dialog.component';
import { ProductDialogComponent } from './product-dialog.component';
import { ServiceDialogComponent, ServiceDialogData } from './service-dialog.component';
import { StockAdjustmentDialogComponent } from './stock-adjustment-dialog.component';

type CatalogTab = 'servicios' | 'productos' | 'categorias';

const CREATE_LABELS: Record<CatalogTab, string> = {
  servicios: 'Nuevo servicio',
  productos: 'Nuevo producto',
  categorias: 'Nueva categoría',
};

/** ?tab=servicios|productos|categorias. ADMIN edits; CASHIER reads active items only. Lists aren't paged: search is client-side. */
@Component({
  selector: 'app-catalog-page',
  imports: [
    MoneyPipe,
    BadgeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    SegmentedTabsComponent,
    UiIconComponent,
  ],
  templateUrl: './catalog-page.component.html',
})
export class CatalogPageComponent {
  private readonly dialog = inject(Dialog);
  private readonly router = inject(Router);
  private readonly api = inject(CatalogApi);
  private readonly confirm = inject(ConfirmService);
  private readonly notifications = inject(NotificationService);

  readonly tab = input<string>();

  protected readonly isAdmin = inject(SessionStore).isAdmin;
  protected readonly activeLabel = activeLabel;
  protected readonly stockLabel = stockLabel;
  protected readonly initials = initials;
  protected readonly createLabels = CREATE_LABELS;

  protected readonly currentTab = computed<CatalogTab>(() => {
    const tab = this.tab();
    if (tab === 'productos') return tab;
    if (tab === 'categorias' && this.isAdmin()) return tab;
    return 'servicios';
  });
  protected readonly tabs = computed<TabItem[]>(() => [
    { id: 'servicios', label: 'Servicios' },
    { id: 'productos', label: 'Productos' },
    ...(this.isAdmin() ? [{ id: 'categorias', label: 'Categorías' }] : []),
  ]);
  protected readonly search = signal('');
  protected readonly showInactive = signal(true);
  private readonly activeOnly = computed(() => !this.isAdmin() || !this.showInactive());

  protected readonly services = rxResource({
    params: () => this.activeOnly(),
    stream: ({ params }) => this.api.services(params),
  });
  protected readonly products = rxResource({
    params: () => this.activeOnly(),
    stream: ({ params }) => this.api.products(params),
  });
  protected readonly categories = rxResource({
    params: () => (this.isAdmin() ? true : undefined),
    stream: () => this.api.categories(),
  });

  private readonly query = computed(() => this.search().trim().toLowerCase());
  protected readonly serviceRows = computed(() =>
    this.services.hasValue()
      ? this.services.value().filter((s) => this.matches(s.name, s.category, s.description))
      : [],
  );
  protected readonly productRows = computed(() =>
    this.products.hasValue()
      ? this.products.value().filter((p) => this.matches(p.name, p.sku))
      : [],
  );
  protected readonly categoryRows = computed(() =>
    this.categories.hasValue() ? this.categories.value().filter((c) => this.matches(c.name)) : [],
  );

  /** Error + loading of the visible tab. */
  protected readonly tabError = computed(() => {
    const error = {
      servicios: this.services,
      productos: this.products,
      categorias: this.categories,
    }[this.currentTab()].error();
    return error ? apiErrorMessage(error) : '';
  });
  protected readonly tabLoading = computed(
    () =>
      !{
        servicios: this.services.hasValue(),
        productos: this.products.hasValue(),
        categorias: this.categories.hasValue(),
      }[this.currentTab()],
  );

  constructor() {
    // A disallowed/unknown ?tab (e.g. CASHIER on categorias) → rewrite the URL to the tab actually shown.
    effect(() => {
      const tab = this.tab();
      if (tab && tab !== this.currentTab()) this.selectTab(this.currentTab(), true);
    });
  }

  private matches(...fields: (string | null)[]): boolean {
    const q = this.query();
    return !q || fields.some((f) => f?.toLowerCase().includes(q));
  }

  protected selectTab(tab: string, replaceUrl = false): void {
    this.search.set('');
    void this.router.navigate([], {
      queryParams: { tab },
      queryParamsHandling: 'merge',
      replaceUrl,
    });
  }

  protected reloadTab(): void {
    ({ servicios: this.services, productos: this.products, categorias: this.categories })[
      this.currentTab()
    ].reload();
  }

  protected create(): void {
    const tab = this.currentTab();
    if (tab === 'servicios') this.editService(null);
    else if (tab === 'productos') this.editProduct(null);
    else this.editCategory(null);
  }

  protected editService(service: SalonServiceResponse | null): void {
    const data: ServiceDialogData = {
      service,
      categories: this.categories.hasValue() ? this.categories.value() : [],
    };
    this.dialog
      .open<SalonServiceResponse>(ServiceDialogComponent, { data })
      .closed.pipe(filter(Boolean))
      .subscribe(() => {
        this.notifications.success('Servicio guardado');
        this.services.reload();
      });
  }

  protected editProduct(product: ProductResponse | null): void {
    this.dialog
      .open<ProductResponse>(ProductDialogComponent, { data: product })
      .closed.pipe(filter(Boolean))
      .subscribe(() => {
        this.notifications.success('Producto guardado');
        this.products.reload();
      });
  }

  protected adjustStock(product: ProductResponse): void {
    this.dialog
      .open<ProductResponse>(StockAdjustmentDialogComponent, { data: product })
      .closed.pipe(filter(Boolean))
      .subscribe((updated) => {
        this.notifications.success(`Stock actualizado: ${updated.stock}`);
        this.products.update((list) => list?.map((p) => (p.id === updated.id ? updated : p)));
      });
  }

  protected editCategory(category: CategoryResponse | null): void {
    this.dialog
      .open<CategoryResponse>(CategoryDialogComponent, { data: category })
      .closed.pipe(filter(Boolean))
      .subscribe(() => {
        this.notifications.success('Categoría guardada');
        this.categories.reload();
        this.services.reload(); // category names are denormalized into services
      });
  }

  protected deleteCategory(category: CategoryResponse): void {
    this.confirm
      .ask({
        title: 'Eliminar categoría',
        message: `¿Eliminar «${category.name}»? Esta acción no se puede deshacer.`,
        confirmLabel: 'Eliminar',
        danger: true,
      })
      .pipe(
        filter(Boolean),
        switchMap(() => this.api.deleteCategory(category.id)),
      )
      .subscribe({
        next: () => {
          this.notifications.success('Categoría eliminada');
          this.categories.update((list) => list?.filter((c) => c.id !== category.id));
        },
        error: (e) =>
          this.notifications.error(
            apiErrorMessage(
              e,
              'No se puede eliminar: tiene servicios asociados. Desactívala en su lugar.',
            ),
          ),
      });
  }
}
