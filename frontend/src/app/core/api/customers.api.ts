import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CustomerQuery, CustomerRequest, CustomerResponse, Page } from '../models/api.models';
import { toHttpParams } from '../http/http-params';

/** ADMIN, CASHIER. */
@Injectable({ providedIn: 'root' })
export class CustomersApi {
  private readonly http = inject(HttpClient);

  /** `q` matches name, phone, email or tax id; sorted by name. */
  list(query: CustomerQuery): Observable<Page<CustomerResponse>> {
    return this.http.get<Page<CustomerResponse>>('customers', { params: toHttpParams(query) });
  }

  get(id: number): Observable<CustomerResponse> {
    return this.http.get<CustomerResponse>(`customers/${id}`);
  }

  create(request: CustomerRequest): Observable<CustomerResponse> {
    return this.http.post<CustomerResponse>('customers', request);
  }

  update(id: number, request: CustomerRequest): Observable<CustomerResponse> {
    return this.http.put<CustomerResponse>(`customers/${id}`, request);
  }
}
