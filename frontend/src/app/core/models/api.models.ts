/** DTOs mirroring the StyloFlow backend (see API_CONTRACT.md). Money = number, Instant = ISO string, LocalDate = 'yyyy-MM-dd'. */

export type Role = 'ADMIN' | 'CASHIER' | 'STYLIST' | 'SUPERADMIN';
export type BusinessRole = Exclude<Role, 'SUPERADMIN'>;
export type PaymentMethod = 'CASH' | 'QR' | 'CARD' | 'TRANSFER';
export type ItemType = 'SERVICE' | 'PRODUCT';
export type SaleStatus = 'COMPLETED' | 'VOIDED';
export type CashRegisterStatus = 'OPEN' | 'CLOSED';

export interface ApiProblem {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  errors?: Record<string, string>;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
}

export interface DateRangeQuery {
  from?: string;
  to?: string;
}

// ── Auth ──────────────────────────────────────────────
export interface LoginRequest {
  businessCode: string;
  username: string;
  password: string;
}

export interface UserResponse {
  id: number;
  name: string;
  username: string;
  role: BusinessRole;
  phone: string | null;
  commissionRate: number | null;
  active: boolean;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: UserResponse;
  business: { code: string; name: string };
}

export interface PlatformLoginRequest {
  username: string;
  password: string;
}

export interface PlatformLoginResponse {
  token: string;
  expiresAt: string;
  name: string;
}

// ── Business ──────────────────────────────────────────
export interface BusinessRequest {
  name: string;
  taxId: string | null;
  address: string | null;
  phone: string | null;
  currency: string;
  currencySymbol: string;
  taxRate: number;
  receiptMessage: string | null;
}

export interface BusinessResponse extends BusinessRequest {
  code: string;
}

// ── Cash register ─────────────────────────────────────
export interface PaymentMethodTotal {
  method: PaymentMethod;
  count: number;
  total: number;
}

export interface OpenRegisterRequest {
  openingAmount: number;
  notes: string | null;
}

export interface CloseRegisterRequest {
  countedCash: number;
  notes: string | null;
}

export interface CashRegisterResponse {
  id: number;
  status: CashRegisterStatus;
  openedBy: string;
  openedAt: string;
  openingAmount: number;
  closedBy: string | null;
  closedAt: string | null;
  salesCount: number;
  salesTotal: number;
  expectedCash: number;
  countedCash: number | null;
  difference: number | null;
  byPaymentMethod: PaymentMethodTotal[];
  notes: string | null;
}

// ── Catalog ───────────────────────────────────────────
export interface CategoryRequest {
  name: string;
  active: boolean;
}

export interface CategoryResponse {
  id: number;
  name: string;
  active: boolean;
}

export interface SalonServiceRequest {
  categoryId: number;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
}

export interface SalonServiceResponse {
  id: number;
  categoryId: number;
  category: string;
  name: string;
  description: string | null;
  durationMinutes: number;
  price: number;
  active: boolean;
  imageUrl: string | null;
}

export interface ProductRequest {
  name: string;
  sku: string | null;
  price: number;
  /** Create only; ignored on update. */
  stock?: number;
  minStock: number | null;
  active: boolean;
}

export interface ProductResponse {
  id: number;
  name: string;
  sku: string | null;
  price: number;
  stock: number;
  minStock: number | null;
  active: boolean;
  lowStock: boolean;
  imageUrl: string | null;
}

// ── Customers ─────────────────────────────────────────
export interface CustomerRequest {
  name: string;
  phone: string | null;
  email: string | null;
  taxId: string | null;
  notes: string | null;
}

export interface CustomerResponse extends CustomerRequest {
  id: number;
  createdAt: string;
}

export interface CustomerQuery extends PageQuery {
  q?: string;
}

// ── Sales ─────────────────────────────────────────────
export interface SaleItemRequest {
  type: ItemType;
  itemId: number;
  quantity: number;
  unitPrice?: number;
  discount?: number;
  stylistId?: number | null;
}

export interface SaleRequest {
  customerId: number | null;
  items: SaleItemRequest[];
  discount: number;
  paymentMethod: PaymentMethod;
  amountReceived?: number;
  notes: string | null;
}

export interface SaleItemResponse {
  id: number;
  type: ItemType;
  itemId: number;
  description: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  subtotal: number;
  stylistId: number | null;
  stylist: string | null;
}

export interface SaleResponse {
  id: number;
  date: string;
  cashRegisterId: number;
  cashier: string;
  customerId: number | null;
  customer: string | null;
  customerTaxId: string | null;
  subtotal: number;
  discount: number;
  total: number;
  tax: number;
  paymentMethod: PaymentMethod;
  amountReceived: number | null;
  change: number | null;
  status: SaleStatus;
  voidedBy: string | null;
  voidedAt: string | null;
  voidReason: string | null;
  notes: string | null;
  items: SaleItemResponse[];
}

export interface SaleSummaryResponse {
  id: number;
  date: string;
  cashier: string;
  customerId: number | null;
  customer: string | null;
  total: number;
  paymentMethod: PaymentMethod;
  status: SaleStatus;
  cashRegisterOpen: boolean;
}

export interface SalesQuery extends PageQuery, DateRangeQuery {
  customerId?: number;
}

// ── Reports ───────────────────────────────────────────
export interface SalesSummaryResponse {
  from: string;
  to: string;
  salesCount: number;
  salesTotal: number;
  averageTicket: number;
  totalDiscounts: number;
  totalTax: number;
  voidedSales: number;
  byPaymentMethod: PaymentMethodTotal[];
}

export interface SalesByDay {
  date: string;
  count: number;
  total: number;
}

export interface StylistCommission {
  stylistId: number;
  stylist: string;
  services: number;
  total: number;
  commissionRate: number;
  commission: number;
}

export interface TopItem {
  id: number;
  name: string;
  quantity: number;
  total: number;
}

// ── Users ─────────────────────────────────────────────
export interface UserRequest {
  name: string;
  username: string;
  /** Required on create; omitted on update = unchanged. */
  password?: string;
  role: BusinessRole;
  phone: string | null;
  commissionRate: number | null;
  active: boolean;
}

export interface StylistOption {
  id: number;
  name: string;
  role: Role;
}

// ── Platform ──────────────────────────────────────────
export interface PlatformBusiness {
  id: number;
  code: string;
  name: string;
  active: boolean;
  createdAt: string;
  users: number;
  sales30d: number;
  total30d: number;
}

export interface CreateBusinessRequest {
  name: string;
  code: string;
  taxId: string | null;
  phone: string | null;
  adminName: string;
  adminUsername: string;
  adminPassword: string;
  baseCatalog: boolean;
}
