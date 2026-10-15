import { ProductResponse, SalonServiceResponse } from '../../core/models/api.models';
import { parseMoney } from '../../core/utils/forms.util';
import {
  addProduct,
  addService,
  cartSubtotal,
  checkoutError,
  includedTax,
  setQuantity,
  validDiscount,
  suggestedBills,
} from './cart';

const service = { id: 1, name: 'Corte', price: 25 } as SalonServiceResponse;
const product = { id: 7, name: 'Shampoo', price: 6.1, stock: 2 } as ProductResponse;

describe('cart', () => {
  it('adds a new line per service and merges products up to stock', () => {
    let lines = addService([], service, 1, null);
    lines = addService(lines, service, 2, 5);
    lines = addProduct(lines, product, 3);
    lines = addProduct(lines, product, 4);
    lines = addProduct(lines, product, 5); // capped at stock 2
    expect(lines.length).toBe(3);
    expect(lines[2].quantity).toBe(2);
    expect(cartSubtotal(lines)).toBe(62.2);
    expect(setQuantity(lines, 3, 0)[2].quantity).toBe(1);
  });

  it('computes included tax and bill suggestions', () => {
    expect(includedTax(113, 13)).toBe(13);
    expect(includedTax(50, 0)).toBe(0);
    expect(suggestedBills(41.44)).toEqual([50, 60]);
    expect(suggestedBills(50)).toEqual([60, 100]);
  });

  it('parses money with comma or dot decimals', () => {
    expect(parseMoney('1.234,50')).toBe(1234.5);
    expect(parseMoney('12.5')).toBe(12.5);
    expect(parseMoney('12,5')).toBe(12.5);
    expect(parseMoney('abc')).toBeNull();
    expect(parseMoney('')).toBeNull();
  });

  it('blocks checkout on invalid discount or insufficient cash', () => {
    const base = { lineCount: 1, subtotal: 50, discountText: '', method: 'CASH' as const };
    expect(checkoutError({ ...base, discountText: '60', receivedText: '50' })).toBe(
      'El descuento no puede superar el subtotal.',
    );
    expect(validDiscount('60', 50)).toBe(0);
    expect(checkoutError({ ...base, discountText: '10', receivedText: '39,99' })).toBe(
      'El monto recibido no cubre el total.',
    );
    expect(checkoutError({ ...base, discountText: '10', receivedText: '40' })).toBe('');
    expect(checkoutError({ ...base, method: 'CARD', receivedText: '' })).toBe('');
  });
});
