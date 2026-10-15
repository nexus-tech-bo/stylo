import {
  Component,
  computed,
  DestroyRef,
  Directive,
  inject,
  input,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NgControl } from '@angular/forms';
import { map, startWith, switchMap } from 'rxjs';
import { controlError } from '../../utils/forms.util';

let nextId = 0;
const errorIds = new WeakMap<AbstractControl, string>();

/** Shared id between a control's input (`aria-describedby`) and its `app-field-error` message. */
function errorIdFor(control: AbstractControl): string {
  let id = errorIds.get(control);
  if (!id) errorIds.set(control, (id = `field-error-${nextId++}`));
  return id;
}

/**
 * Spanish error of a touched control. Driven by `control.events` (value/status/touched), so it also
 * updates when errors are set from an HTTP callback (zoneless: no event → no change detection otherwise).
 */
@Component({
  selector: 'app-field-error',
  template: `
    @if (message(); as text) {
      <p class="field-error" [id]="id()">{{ text }}</p>
    }
  `,
})
export class FieldErrorComponent {
  readonly control = input.required<AbstractControl>();
  protected readonly id = computed(() => errorIdFor(this.control()));
  protected readonly message = toSignal(
    toObservable(this.control).pipe(
      switchMap((control) =>
        control.events.pipe(
          startWith(null),
          map(() => controlError(control)),
        ),
      ),
    ),
    { initialValue: null },
  );
}

/** Marks a reactive-forms input invalid for screen readers and links it to its `app-field-error`. */
@Directive({
  selector: '[formControlName],[formControl]',
  host: {
    '[attr.aria-invalid]': 'invalid() || null',
    '[attr.aria-describedby]': 'invalid() ? errorId : null',
  },
})
export class FieldErrorA11yDirective implements OnInit {
  private readonly ngControl = inject(NgControl);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly invalid = signal(false);
  protected errorId: string | null = null;

  ngOnInit(): void {
    const control = this.ngControl.control;
    if (!control) return;
    this.errorId = errorIdFor(control);
    control.events
      .pipe(startWith(null), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalid.set(!!controlError(control)));
  }
}

/** Import this in forms: the message component plus the input-side aria wiring. */
export const FIELD_ERROR = [FieldErrorComponent, FieldErrorA11yDirective] as const;
