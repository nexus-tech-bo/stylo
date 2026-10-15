import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CategoryRequest,
  CategoryResponse,
  ProductRequest,
  ProductResponse,
  SalonServiceRequest,
  SalonServiceResponse,
} from '../models/api.models';
import { toFormData, toHttpParams } from '../http/http-params';

/** Reads: any staff role. Writes: ADMIN. */
@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  categories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>('catalog/categories');
  }

  createCategory(request: CategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>('catalog/categories', request);
  }

  updateCategory(id: number, request: CategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`catalog/categories/${id}`, request);
  }

  /** Fails (409/422) if the category has services. */
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`catalog/categories/${id}`);
  }

  services(activeOnly: boolean): Observable<SalonServiceResponse[]> {
    return this.http.get<SalonServiceResponse[]>('catalog/services', {
      params: toHttpParams({ activeOnly }),
    });
  }

  /** Multipart; the image (≤5 MB) can only be set on create. */
  createService(
    request: SalonServiceRequest,
    image: File | null,
  ): Observable<SalonServiceResponse> {
    return this.http.post<SalonServiceResponse>('catalog/services', toFormData(request, image));
  }

  /** JSON; image unchanged. */
  updateService(id: number, request: SalonServiceRequest): Observable<SalonServiceResponse> {
    return this.http.put<SalonServiceResponse>(`catalog/services/${id}`, request);
  }

  products(activeOnly: boolean): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>('catalog/products', {
      params: toHttpParams({ activeOnly }),
    });
  }

  lowStock(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>('catalog/products/low-stock');
  }

  /** Multipart; `stock` required here. */
  createProduct(request: ProductRequest, image: File | null): Observable<ProductResponse> {
    return this.http.post<ProductResponse>('catalog/products', toFormData(request, image));
  }

  /** JSON; stock and image ignored. */
  updateProduct(id: number, request: ProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`catalog/products/${id}`, request);
  }

  /** Signed quantity: + adds, − removes. */
  adjustStock(id: number, quantity: number): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(`catalog/products/${id}/stock-adjustment`, { quantity });
  }
}
