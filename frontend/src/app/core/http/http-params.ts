import { HttpParams } from '@angular/common/http';

/** Builds HttpParams skipping null/undefined/'' so optional filters are simply omitted. */
export function toHttpParams(query: object = {}): HttpParams {
  let params = new HttpParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === null || value === undefined || value === '') continue;
    params = params.set(key, String(value));
  }
  return params;
}

/** Multipart body for the catalog create endpoints: one part per field + optional image. */
export function toFormData(fields: object, image: File | null): FormData {
  const body = new FormData();
  for (const [key, value] of Object.entries(fields)) {
    if (value === null || value === undefined) continue;
    body.append(key, String(value));
  }
  if (image) body.append('image', image);
  return body;
}

export const PAGE_SIZE = 20;

/** `?page=` query param → 0-based page index (invalid/missing → 0). */
export const pageIndex = (raw?: string | null): number => Number(raw ?? 0) || 0;
