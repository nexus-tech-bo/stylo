import { TestBed } from '@angular/core/testing';
import { ListDatePipe } from './list-date.pipe';

describe('ListDatePipe', () => {
  it('omits the year only for the current year', () => {
    const pipe = TestBed.runInInjectionContext(() => new ListDatePipe());
    const year = new Date().getFullYear();
    expect(pipe.transform(new Date(year, 2, 5, 9, 7).toISOString())).toBe('05/03 09:07');
    expect(pipe.transform(new Date(2020, 2, 5, 9, 7).toISOString())).toBe('05/03/20 09:07');
    expect(pipe.transform(null)).toBeNull();
  });
});
