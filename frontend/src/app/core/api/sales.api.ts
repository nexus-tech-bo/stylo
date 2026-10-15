import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Page,
  SaleRequest,
  SaleResponse,
  SalesQuery,
  SaleSummaryResponse,
} from '../models/api.models';
import { toHttpParams } from '../http/http-params';

/** ADMIN, CASHIER. */
@Injectable({ providedIn: 'root' })
export class SalesApi {
  private readonly http = inject(HttpClient);

  /** Needs an open register (422 otherwise). */
  create(request: SaleRequest): Observable<SaleResponse> {
    return this.http.post<SaleResponse>('sales', request);
  }

  /** Defaults to today, sorted by createdAt desc. */
  list(query: SalesQuery): Observable<Page<SaleSummaryResponse>> {
    return this.http.get<Page<SaleSummaryResponse>>('sales', { params: toHttpParams(query) });
  }

  get(id: number): Observable<SaleResponse> {
    return this.http.get<SaleResponse>(`sales/${id}`);
  }

  /** ADMIN; only sales of the open register. Restores product stock. */
  voidSale(id: number, reason: string): Observable<SaleResponse> {
    return this.http.post<SaleResponse>(`sales/${id}/void`, { reason });
  }
}
