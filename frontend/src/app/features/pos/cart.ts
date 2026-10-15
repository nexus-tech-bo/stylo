import {
  ItemType,
  PaymentMethod,
  ProductResponse,
  SalonServiceResponse,
} from '../../core/models/api.models';
import { parseMoney, roundMoney } from '../../core/utils/forms.util';

export interface CartLine {
  key: number;
  type: ItemType;
  itemId: number;
  name: string;
  unitPrice: number;
  quantity: number;
  stylistId: number | null;
  /** Products only: available stock caps the quantity. */
  maxStock: number | null;
}

export const MAX_QUANTITY = 999;

/** A service always adds a new line (each may have a different stylist). */
export function addService(
  lines: CartLine[],
  service: SalonServiceResponse,
  key: number,
  stylistId: number | null,
): CartLine[] {
  return [
    ...lines,
    {
      key,
      type: 'SERVICE',
      itemId: service.id,
      name: service.name,
      unitPrice: service.price,
      quantity: 1,
      stylistId,
      maxStock: null,
    },
  ];
}

/** A product already in the cart increments its quantity (capped by stock). */
export function addProduct(lines: CartLine[], product: ProductResponse, key: number): CartLine[] {
  const existing = lines.find((l) => l.type === 'PRODUCT' && l.itemId === product.id);
  if (existing) return setQuantity(lines, existing.key, existing.quantity + 1);
  if (product.stock <= 0) return lines;
  return [
    ...lines,
    {
      key,
      type: 'PRODUCT',
      itemId: product.id,
      name: product.name,
      unitPrice: product.price,
      quantity: 1,
      stylistId: null,
      maxStock: product.stock,
    },
  ];
}

export function maxQuantity(line: CartLine): number {
  return Math.min(MAX_QUANTITY, line.maxStock ?? MAX_QUANTITY);
}

export function setQuantity(lines: CartLine[], key: number, quantity: number): CartLine[] {
  return lines.map((l) =>
    l.key === key ? { ...l, quantity: Math.max(1, Math.min(maxQuantity(l), quantity)) } : l,
  );
}

export function cartSubtotal(lines: CartLine[]): number {
  return roundMoney(lines.reduce((sum, l) => sum + l.unitPrice * l.quantity, 0));
}

/** Tax is included in prices: the informative part of `total` that is tax. */
export function includedTax(total: number, taxRate: number): number {
  return taxRate > 0 ? roundMoney((total * taxRate) / (100 + taxRate)) : 0;
}

/** Up to two round bills above the total for the "Recibido" quick buttons. */
export function suggestedBills(total: number): number[] {
  const bills = [10, 20, 50, 100, 200, 500, 1000]
    .map((b) => Math.ceil(total / b) * b)
    .filter((v) => v > total);
  return [...new Set(bills)].sort((a, b) => a - b).slice(0, 2);
}

const INVALID_AMOUNT = 'Ingresa un monto válido.';

/** Discount field error ('' = empty or valid). */
export function discountError(text: string, subtotal: number): string {
  if (!text.trim()) return '';
  const value = parseMoney(text);
  if (value === null || value < 0) return INVALID_AMOUNT;
  return value > subtotal ? 'El descuento no puede superar el subtotal.' : '';
}

/** Valid discount amount; an invalid one is ignored (0) so previews stay meaningful. */
export function validDiscount(text: string, subtotal: number): number {
  return discountError(text, subtotal) ? 0 : (parseMoney(text) ?? 0);
}

/** Cash "Recibido" field error ('' = empty or covers the total). */
export function receivedError(text: string, total: number): string {
  if (!text.trim()) return '';
  const value = parseMoney(text);
  if (value === null) return INVALID_AMOUNT;
  return value < total ? 'El monto recibido no cubre el total.' : '';
}

export interface Checkout {
  lineCount: number;
  subtotal: number;
  discountText: string;
  method: PaymentMethod;
  receivedText: string;
}

/** Why the sale can't be charged yet ('' = can charge). Tax is included: total = subtotal − discount. */
export function checkoutError(c: Checkout): string {
  if (!c.lineCount) return 'Agrega al menos un servicio o producto.';
  const discount = discountError(c.discountText, c.subtotal);
  if (discount) return discount;
  if (c.method !== 'CASH') return '';
  if (parseMoney(c.receivedText) === null) return 'Ingresa el monto recibido.';
  return receivedError(
    c.receivedText,
    roundMoney(c.subtotal - validDiscount(c.discountText, c.subtotal)),
  );
}
