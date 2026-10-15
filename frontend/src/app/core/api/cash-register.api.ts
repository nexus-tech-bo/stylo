import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import {
  CashRegisterResponse,
  CloseRegisterRequest,
  OpenRegisterRequest,
  Page,
  PageQuery,
} from '../models/api.models';
import { toHttpParams } from '../http/http-params';

/** ADMIN, CASHIER. */
@Injectable({ providedIn: 'root' })
export class CashRegisterApi {
  private readonly http = inject(HttpClient);

  /** The open register, or `null` when none is open (204). */
  current(): Observable<CashRegisterResponse | null> {
    return this.http
      .get<CashRegisterResponse | null>('cash-register/current')
      .pipe(map((r) => r ?? null));
  }

  open(request: OpenRegisterRequest): Observable<CashRegisterResponse> {
    return this.http.post<CashRegisterResponse>('cash-register/open', request);
  }

  close(request: CloseRegisterRequest): Observable<CashRegisterResponse> {
    return this.http.post<CashRegisterResponse>('cash-register/close', request);
  }

  /** Sorted by openedAt desc by default. */
  history(query: PageQuery): Observable<Page<CashRegisterResponse>> {
    return this.http.get<Page<CashRegisterResponse>>('cash-register/history', {
      params: toHttpParams(query),
    });
  }
}
