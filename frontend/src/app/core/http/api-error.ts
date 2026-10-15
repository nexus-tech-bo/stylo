import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem } from '../models/api.models';

/** The single translator for backend errors (backend messages are English → Spanish UI copy). */
export interface ApiErrorInfo {
  status: number;
  title: string;
  detail: string;
  /** Spanish message for a banner/toast. */
  message: string;
  /** 400 `Invalid data` field errors, translated. */
  fieldErrors: Record<string, string>;
}

export const SUSPENDED_TITLE = 'Business suspended';
export const NO_OPEN_REGISTER = 'There is no open cash register';

export const MSG = {
  network: 'No se pudo conectar con el servidor. Intenta de nuevo.',
  server: 'Algo salió mal. Intenta de nuevo.',
  expired: 'Tu sesión expiró. Inicia sesión de nuevo.',
  suspended: 'Este salón está suspendido. Contacta al soporte de StyloFlow.',
  forbidden: 'No tienes permiso para realizar esta acción.',
  notFound: 'No encontramos lo que buscas.',
  invalidData: 'Revisa los campos marcados.',
  invalidRequest: 'La solicitud no es válida. Revisa los datos e intenta de nuevo.',
  conflict: 'Ya existe un registro con esos datos.',
  rule: 'No se pudo completar la operación. Revisa los datos e intenta de nuevo.',
} as const;

/** Known business-rule details (422/409) → Spanish. Matched case-insensitively by substring. */
const KNOWN_DETAILS: [string, string][] = [
  [NO_OPEN_REGISTER, 'No hay una caja abierta. Abre la caja para registrar ventas.'],
  ['A cash register is already open', 'Ya existe una caja abierta.'],
  ['The file must be an image', 'El archivo debe ser una imagen.'],
  ['The image must be at most 5 MB', 'La imagen supera 5 MB.'],
  [
    'Image storage is not configured',
    'El almacenamiento de imágenes no está configurado. La imagen no se pudo guardar.',
  ],
];

/** Common Bean Validation messages → Spanish; anything else is shown as sent. */
const FIELD_MESSAGES: [RegExp, (m: RegExpMatchArray) => string][] = [
  [/must not be (blank|null|empty)/i, () => 'Este campo es obligatorio.'],
  [
    /size must be between (\d+) and (\d+)/i,
    (m) => `Debe tener entre ${m[1]} y ${m[2]} caracteres.`,
  ],
  [/must be greater than or equal to (\S+)/i, (m) => `Debe ser mayor o igual a ${m[1]}.`],
  [/must be less than or equal to (\S+)/i, (m) => `Debe ser menor o igual a ${m[1]}.`],
  [/must be greater than (\S+)/i, (m) => `Debe ser mayor a ${m[1]}.`],
  [/well-formed email/i, () => 'Ingresa un email válido.'],
  [/must match/i, () => 'El formato no es válido.'],
];

function translateDetail(detail: string): string | null {
  const lower = detail.toLowerCase();
  return KNOWN_DETAILS.find(([en]) => lower.includes(en.toLowerCase()))?.[1] ?? null;
}

function translateField(message: string): string {
  for (const [re, fn] of FIELD_MESSAGES) {
    const match = message.match(re);
    if (match) return fn(match);
  }
  return message;
}

/** rxResource wraps non-Error values; unwrap to reach the HttpErrorResponse. */
function unwrap(error: unknown): unknown {
  if (error instanceof HttpErrorResponse) return error;
  if (error instanceof Error && error.cause) return unwrap(error.cause);
  return error;
}

/**
 * @param ruleFallback message for unknown 409/422 details (context-specific), defaults to a generic one.
 */
export function apiError(error: unknown, ruleFallback?: string): ApiErrorInfo {
  const http = unwrap(error);
  if (!(http instanceof HttpErrorResponse)) {
    return { status: -1, title: '', detail: '', message: MSG.server, fieldErrors: {} };
  }
  const body: ApiProblem =
    http.error && typeof http.error === 'object' && !(http.error instanceof Blob) ? http.error : {};
  const title = body.title ?? '';
  const detail = body.detail ?? '';
  const fieldErrors: Record<string, string> = {};
  for (const [field, msg] of Object.entries(body.errors ?? {}))
    fieldErrors[field] = translateField(msg);

  const info = (message: string): ApiErrorInfo => ({
    status: http.status,
    title,
    detail,
    message,
    fieldErrors,
  });
  switch (http.status) {
    case 0:
    case 502: // gateway errors: the backend is unreachable (e.g. dev proxy with the API down)
    case 503:
    case 504:
      return info(MSG.network);
    case 400:
      return info(
        Object.keys(fieldErrors).length
          ? MSG.invalidData
          : (translateDetail(detail) ?? MSG.invalidRequest),
      );
    case 401:
      return info(MSG.expired);
    case 403:
      return info(title === SUSPENDED_TITLE ? MSG.suspended : MSG.forbidden);
    case 404:
      return info(MSG.notFound);
    case 409:
      return info(translateDetail(detail) ?? ruleFallback ?? MSG.conflict);
    case 413:
      return info('La imagen supera 5 MB.');
    case 422:
      return info(translateDetail(detail) ?? ruleFallback ?? MSG.rule);
    default:
      return info(MSG.server);
  }
}

export function apiErrorMessage(error: unknown, ruleFallback?: string): string {
  return apiError(error, ruleFallback).message;
}

export function isNoOpenRegister(error: unknown): boolean {
  const { status, detail } = apiError(error);
  return status === 422 && detail.toLowerCase().includes(NO_OPEN_REGISTER.toLowerCase());
}

export function isSuspended(error: unknown): boolean {
  const { status, title } = apiError(error);
  return status === 403 && title === SUSPENDED_TITLE;
}
