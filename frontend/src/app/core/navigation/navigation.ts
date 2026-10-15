import { BusinessRole, Role } from '../models/api.models';

/** Route role sets, mirroring backend authorization. Keep in sync with app.routes.ts. */
export const ROLES = {
  staff: ['ADMIN', 'CASHIER'],
  admin: ['ADMIN'],
  stylist: ['STYLIST'],
} as const satisfies Record<string, readonly BusinessRole[]>;

export interface NavItem {
  label: string;
  icon: string;
  path: string;
  roles: readonly Role[];
}

export const BUSINESS_NAV: NavItem[] = [
  { label: 'Inicio', icon: 'home', path: '/inicio', roles: ROLES.staff },
  { label: 'Nueva venta', icon: 'cart', path: '/venta', roles: ROLES.staff },
  { label: 'Ventas', icon: 'receipt', path: '/ventas', roles: ROLES.staff },
  { label: 'Caja', icon: 'cash', path: '/caja', roles: ROLES.staff },
  { label: 'Clientes', icon: 'users', path: '/clientes', roles: ROLES.staff },
  { label: 'Catálogo', icon: 'scissors', path: '/catalogo', roles: ROLES.staff },
  { label: 'Reportes', icon: 'chart', path: '/reportes', roles: ROLES.staff },
  { label: 'Mis comisiones', icon: 'percent', path: '/mis-comisiones', roles: ROLES.stylist },
  { label: 'Usuarios', icon: 'staff', path: '/usuarios', roles: ROLES.admin },
  { label: 'Ajustes del negocio', icon: 'settings', path: '/ajustes', roles: ROLES.admin },
];

export const PLATFORM_HOME = '/plataforma/negocios';

export const PLATFORM_NAV: NavItem[] = [
  { label: 'Negocios', icon: 'building', path: PLATFORM_HOME, roles: ['SUPERADMIN'] },
];

export function homePath(role: Role | null): string {
  return role === 'STYLIST' ? '/mis-comisiones' : '/inicio';
}
