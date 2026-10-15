import { HttpClient, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DateRangeQuery,
  ItemType,
  SalesByDay,
  SalesSummaryResponse,
  StylistCommission,
  TopItem,
} from '../models/api.models';
import { toHttpParams } from '../http/http-params';

/** All ranges default to today on the server; max 1 year (422). */
@Injectable({ providedIn: 'root' })
export class ReportsApi {
  private readonly http = inject(HttpClient);

  /** ADMIN, CASHIER. */
  summary(range: DateRangeQuery = {}): Observable<SalesSummaryResponse> {
    return this.http.get<SalesSummaryResponse>('reports/summary', { params: toHttpParams(range) });
  }

  /** ADMIN. */
  salesByDay(range: DateRangeQuery): Observable<SalesByDay[]> {
    return this.http.get<SalesByDay[]>('reports/sales-by-day', { params: toHttpParams(range) });
  }

  /** ADMIN. */
  stylists(range: DateRangeQuery): Observable<StylistCommission[]> {
    return this.http.get<StylistCommission[]>('reports/stylists', { params: toHttpParams(range) });
  }

  /** Any business role; the caller's own rows. */
  myCommissions(range: DateRangeQuery): Observable<StylistCommission[]> {
    return this.http.get<StylistCommission[]>('reports/my-commissions', {
      params: toHttpParams(range),
    });
  }

  /** ADMIN. */
  top(type: ItemType, range: DateRangeQuery, limit = 10): Observable<TopItem[]> {
    return this.http.get<TopItem[]>('reports/top', {
      params: toHttpParams({ ...range, type, limit }),
    });
  }

  /** ADMIN. CSV download; filename in Content-Disposition. */
  salesCsv(range: DateRangeQuery): Observable<HttpResponse<Blob>> {
    return this.http.get('reports/sales.csv', {
      params: toHttpParams(range),
      responseType: 'blob',
      observe: 'response',
    });
  }
}
