import { Directive, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';

@Directive({
  selector: 'input[appTrimInput], textarea[appTrimInput]',
  standalone: true,
})
export class TrimInputDirective {
  private readonly ngControl = inject(NgControl, { optional: true });

  @HostListener('blur', ['$event.target'])
  onBlur(target: EventTarget | null): void {
    if (!(target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement)) {
      return;
    }
    const trimmed = target.value.trim();
    if (trimmed !== target.value) {
      target.value = trimmed;
      this.ngControl?.control?.setValue(trimmed);
    }
  }
}
