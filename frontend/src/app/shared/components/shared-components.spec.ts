import { TestBed } from '@angular/core/testing';
import { SafeEmptyPipe } from '../pipes/safe-empty.pipe';
import { EmptyStateComponent } from './empty-state/empty-state';
import { ErrorStateComponent } from './error-state/error-state';
import { LoadingIndicatorComponent } from './loading-indicator/loading-indicator';
import { PageHeaderComponent } from './page-header/page-header';

describe('Shared UI Components & Pipes', () => {
  it('renders PageHeaderComponent title, subtitle, and badge', () => {
    const fixture = TestBed.createComponent(PageHeaderComponent);
    fixture.componentRef.setInput('title', 'Patients Directory');
    fixture.componentRef.setInput('subtitle', 'Manage patient records');
    fixture.componentRef.setInput('badge', 'Phase 2');
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.page-header__title')?.textContent).toContain('Patients Directory');
    expect(el.querySelector('.page-header__subtitle')?.textContent).toContain('Manage patient records');
    expect(el.querySelector('.page-header__badge')?.textContent).toContain('Phase 2');
  });

  it('renders EmptyStateComponent and emits action event', () => {
    const fixture = TestBed.createComponent(EmptyStateComponent);
    fixture.componentRef.setInput('title', 'No Records');
    fixture.componentRef.setInput('description', 'Module reserved for future phase');
    fixture.componentRef.setInput('actionLabel', 'Go Back');
    fixture.detectChanges();

    let clicked = false;
    fixture.componentInstance.action.subscribe(() => {
      clicked = true;
    });

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    button.click();
    expect(clicked).toBe(true);
  });

  it('renders ErrorStateComponent with correlation reference and emits retry', () => {
    const fixture = TestBed.createComponent(ErrorStateComponent);
    fixture.componentRef.setInput('title', 'Connection Error');
    fixture.componentRef.setInput('message', 'Backend unavailable');
    fixture.componentRef.setInput('correlationId', 'corr-999');
    fixture.detectChanges();

    let retried = false;
    fixture.componentInstance.retry.subscribe(() => {
      retried = true;
    });

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.error-state__reference')?.textContent).toContain('corr-999');
    (el.querySelector('button') as HTMLButtonElement).click();
    expect(retried).toBe(true);
  });

  it('renders LoadingIndicatorComponent in inline and bar modes', () => {
    const fixture = TestBed.createComponent(LoadingIndicatorComponent);
    fixture.componentRef.setInput('mode', 'bar');
    fixture.componentRef.setInput('message', 'Fetching');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-progress-bar')).toBeTruthy();

    fixture.componentRef.setInput('mode', 'inline');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-progress-spinner')).toBeTruthy();
  });

  it('transforms null/blank values via SafeEmptyPipe', () => {
    const pipe = new SafeEmptyPipe();
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform('   ')).toBe('—');
    expect(pipe.transform('Active')).toBe('Active');
  });
});
