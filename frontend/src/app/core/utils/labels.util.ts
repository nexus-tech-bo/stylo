import {
  CashRegisterStatus,
  ItemType,
  PaymentMethod,
  ProductResponse,
  Role,
  SaleStatus,
} from '../models/api.models';

export type Tone = 'success' | 'primary' | 'secondary' | 'tertiary' | 'neutral' | 'danger';

export interface LabelTone {
  label: string;
  tone: Tone;
}

export const ROLE_LABELS: Record<Role, LabelTone> = {
  ADMIN: { label: 'Administrador', tone: 'primary' },
  CASHIER: { label: 'Cajero', tone: 'tertiary' },
  STYLIST: { label: 'Estilista', tone: 'secondary' },
  SUPERADMIN: { label: 'Superadmin', tone: 'neutral' },
};

export const PAYMENT_LABELS: Record<PaymentMethod, string> = {
  CASH: 'Efectivo',
  QR: 'QR',
  CARD: 'Tarjeta',
  TRANSFER: 'Transferencia',
};

export const PAYMENT_METHODS = Object.keys(PAYMENT_LABELS) as PaymentMethod[];

export const SALE_STATUS_LABELS: Record<SaleStatus, LabelTone> = {
  COMPLETED: { label: 'Completada', tone: 'success' },
  VOIDED: { label: 'Anulada', tone: 'danger' },
};

export const REGISTER_STATUS_LABELS: Record<CashRegisterStatus, LabelTone> = {
  OPEN: { label: 'Abierta', tone: 'success' },
  CLOSED: { label: 'Cerrada', tone: 'neutral' },
};

export const ITEM_TYPE_LABELS: Record<ItemType, LabelTone> = {
  SERVICE: { label: 'Servicio', tone: 'primary' },
  PRODUCT: { label: 'Producto', tone: 'tertiary' },
};

export function activeLabel(active: boolean, feminine = false): LabelTone {
  return active
    ? { label: feminine ? 'Activa' : 'Activo', tone: 'success' }
    : { label: feminine ? 'Inactiva' : 'Inactivo', tone: 'neutral' };
}

export function businessStatusLabel(active: boolean): LabelTone {
  return active ? { label: 'Activo', tone: 'success' } : { label: 'Suspendido', tone: 'danger' };
}

export function stockLabel(product: Pick<ProductResponse, 'stock' | 'lowStock'>): LabelTone | null {
  if (product.stock <= 0) return { label: 'Sin stock', tone: 'danger' };
  return product.lowStock ? { label: 'Stock bajo', tone: 'tertiary' } : null;
}

/** Register difference: sign + word + tone (never color alone). */
export function differenceLabel(difference: number): LabelTone {
  if (difference > 0) return { label: 'Sobrante', tone: 'success' };
  if (difference < 0) return { label: 'Faltante', tone: 'danger' };
  return { label: 'Cuadra', tone: 'neutral' };
}

export function initials(name: string | null | undefined): string {
  return (name ?? '')
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
}
