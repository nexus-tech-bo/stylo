import { Routes } from '@angular/router';
import {
  businessAuthGuard,
  homeRedirect,
  loginGuard,
  platformAuthGuard,
  roleGuard,
} from './core/auth/auth.guards';
import { ROLES } from './core/navigation/navigation';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión',
    canActivate: [loginGuard],
    loadComponent: () =>
      import('./features/login/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'plataforma/login',
    title: 'Plataforma',
    data: { platform: true },
    canActivate: [loginGuard],
    loadComponent: () =>
      import('./features/login/login-page.component').then((m) => m.LoginPageComponent),
  },
  {
    path: 'plataforma',
    canMatch: [platformAuthGuard],
    data: { platform: true },
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'negocios' },
      {
        path: 'negocios',
        title: 'Negocios',
        loadComponent: () =>
          import('./features/platform/businesses-page.component').then(
            (m) => m.BusinessesPageComponent,
          ),
      },
    ],
  },
  {
    path: '',
    canMatch: [businessAuthGuard],
    loadComponent: () => import('./layout/shell/shell.component').then((m) => m.ShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: homeRedirect },
      {
        path: 'inicio',
        title: 'Inicio',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/dashboard/dashboard-page.component').then(
            (m) => m.DashboardPageComponent,
          ),
      },
      {
        path: 'venta',
        title: 'Nueva venta',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/pos/pos-page.component').then((m) => m.PosPageComponent),
      },
      {
        path: 'ventas',
        title: 'Ventas',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/sales/sales-list-page.component').then(
            (m) => m.SalesListPageComponent,
          ),
      },
      {
        path: 'ventas/:id',
        title: 'Detalle de venta',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/sales/sale-detail-page.component').then(
            (m) => m.SaleDetailPageComponent,
          ),
      },
      {
        path: 'caja',
        title: 'Caja',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/cash-register/cash-register-page.component').then(
            (m) => m.CashRegisterPageComponent,
          ),
      },
      {
        path: 'clientes',
        title: 'Clientes',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/customers/customers-page.component').then(
            (m) => m.CustomersPageComponent,
          ),
      },
      {
        path: 'catalogo',
        title: 'Catálogo',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/catalog/catalog-page.component').then((m) => m.CatalogPageComponent),
      },
      {
        path: 'reportes',
        title: 'Reportes',
        data: { roles: ROLES.staff },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/reports/reports-page.component').then((m) => m.ReportsPageComponent),
      },
      {
        path: 'mis-comisiones',
        title: 'Mis comisiones',
        data: { roles: ROLES.stylist },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/my-commissions/my-commissions-page.component').then(
            (m) => m.MyCommissionsPageComponent,
          ),
      },
      {
        path: 'usuarios',
        title: 'Usuarios',
        data: { roles: ROLES.admin },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/users/users-page.component').then((m) => m.UsersPageComponent),
      },
      {
        path: 'ajustes',
        title: 'Ajustes del negocio',
        data: { roles: ROLES.admin },
        canActivate: [roleGuard],
        loadComponent: () =>
          import('./features/settings/business-settings-page.component').then(
            (m) => m.BusinessSettingsPageComponent,
          ),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
