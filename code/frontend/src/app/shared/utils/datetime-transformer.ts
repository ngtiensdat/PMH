import { Injectable } from '@angular/core';
import { TuiValueTransformer, TuiDay, TuiTime } from '@taiga-ui/cdk';

@Injectable({ providedIn: 'root' })
export class DateTimeTransformer implements TuiValueTransformer<[TuiDay, TuiTime | null] | null, string> {
  fromControlValue(controlValue: string | null): [TuiDay, TuiTime | null] | null {
    if (!controlValue) return null;
    const d = new Date(controlValue);
    if (isNaN(d.getTime())) return null;
    return [TuiDay.fromLocalNativeDate(d), TuiTime.fromLocalNativeDate(d)];
  }

  toControlValue(componentValue: [TuiDay, TuiTime | null] | null): string {
    if (!componentValue) return '';
    const [tuiDay, tuiTime] = componentValue;
    const d = tuiDay.toLocalNativeDate();
    if (tuiTime) {
      d.setHours(tuiTime.hours);
      d.setMinutes(tuiTime.minutes);
      d.setSeconds(tuiTime.seconds);
    } else {
      d.setHours(0);
      d.setMinutes(0);
      d.setSeconds(0);
    }
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }
}
