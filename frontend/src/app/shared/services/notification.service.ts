import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  kind: 'success' | 'error';
  message: string;
}

const MAX_TOASTS = 3;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private nextId = 0;
  readonly toasts = signal<Toast[]>([]);

  success(message: string): void {
    this.push('success', message, 4000);
  }

  error(message: string): void {
    this.push('error', message, 6000);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private push(kind: Toast['kind'], message: string, ttl: number): void {
    const id = this.nextId++;
    this.toasts.update((list) => [...list, { id, kind, message }].slice(-MAX_TOASTS));
    setTimeout(() => this.dismiss(id), ttl);
  }
}
