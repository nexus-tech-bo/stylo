import { formatDate } from '@angular/common';
import { inject, LOCALE_ID, Pipe, PipeTransform } from '@angular/core';

/** List timestamps: 'dd/MM HH:mm', with the year only when it isn't the current one. */
@Pipe({ name: 'listDate' })
export class ListDatePipe implements PipeTransform {
  private readonly locale = inject(LOCALE_ID);

  transform(value: string | null | undefined): string | null {
    if (!value) return null;
    const sameYear = new Date(value).getFullYear() === new Date().getFullYear();
    return formatDate(value, sameYear ? 'dd/MM HH:mm' : 'dd/MM/yy HH:mm', this.locale);
  }
}
