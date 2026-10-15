import { DatePipe } from '@angular/common';
import { Component, input } from '@angular/core';
import { CashRegisterResponse } from '../../core/models/api.models';
import {
  differenceLabel,
  PAYMENT_LABELS,
  REGISTER_STATUS_LABELS,
} from '../../core/utils/labels.util';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { BadgeComponent } from '../../shared/ui/badge/badge.component';

/** "Turno" layout: status, KPIs, payment methods and (when closed) the count result. Used by the page and the detail dialog. */
@Component({
  selector: 'app-register-summary',
  imports: [DatePipe, MoneyPipe, BadgeComponent],
  templateUrl: './register-summary.component.html',
  host: { class: 'block space-y-4' },
})
export class RegisterSummaryComponent {
  readonly register = input.required<CashRegisterResponse>();
  protected readonly statusLabels = REGISTER_STATUS_LABELS;
  protected readonly paymentLabels = PAYMENT_LABELS;
  protected readonly differenceLabel = differenceLabel;
}
