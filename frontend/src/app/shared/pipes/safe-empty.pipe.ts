import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'safeEmpty',
  standalone: true,
})
export class SafeEmptyPipe implements PipeTransform {
  transform(value: string | number | null | undefined, fallback = '—'): string {
    if (value === null || value === undefined) {
      return fallback;
    }
    const stringValue = String(value).trim();
    return stringValue.length > 0 ? stringValue : fallback;
  }
}
