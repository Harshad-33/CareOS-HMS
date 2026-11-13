import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HospitalRegistrationComponent } from './hospital-registration';

describe('HospitalRegistrationComponent', () => {
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HospitalRegistrationComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should initialize form as invalid when fields are empty', () => {
    const fixture = TestBed.createComponent(HospitalRegistrationComponent);
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    expect(comp.form.valid).toBe(false);
  });

  it('should validate strong password requirements', () => {
    const fixture = TestBed.createComponent(HospitalRegistrationComponent);
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    comp.form.patchValue({ adminPassword: 'weak' });
    expect(comp.form.get('adminPassword')?.hasError('pattern')).toBe(true);

    comp.form.patchValue({ adminPassword: 'SecurePassword!123' });
    expect(comp.form.get('adminPassword')?.valid).toBe(true);
  });

  it('should submit registration request and navigate to hospital list on success', () => {
    const fixture = TestBed.createComponent(HospitalRegistrationComponent);
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    comp.form.setValue({
      hospitalName: 'St. Jude General Hospital',
      hospitalCode: 'HOSP-STJUDE',
      registrationNumber: 'REG-KL-2024-9988',
      hospitalType: 'GENERAL',
      email: 'stjude@hospital.com',
      primaryContactNumber: '+919988776655',
      alternateContactNumber: '',
      website: 'https://stjude.org',
      addressLine1: '45 Church Street',
      addressLine2: '',
      city: 'Kochi',
      district: 'Ernakulam',
      state: 'Kerala',
      country: 'India',
      pinCode: '682001',
      establishedDate: '2020-01-01',
      adminUsername: 'stjude.admin',
      adminEmail: 'admin@stjude.org',
      adminFirstName: 'Francis',
      adminLastName: 'Xavier',
      adminPassword: 'SecurePassword!123',
    });

    expect(comp.form.valid).toBe(true);
    comp.onSubmit();

    const req = httpMock.expectOne((r) => r.url.endsWith('/hospitals') && r.method === 'POST');
    expect(req.request.body.hospitalName).toBe('St. Jude General Hospital');

    req.flush({
      success: true,
      message: 'Hospital registered successfully',
      timestamp: new Date().toISOString(),
      data: {
        id: 5,
        publicId: 'hosp-5',
        hospitalCode: 'HOSP-STJUDE',
        name: 'St. Jude General Hospital',
        status: 'PENDING',
      },
    });

    expect(router.navigate).toHaveBeenCalledWith(['/hospital']);
  });
});