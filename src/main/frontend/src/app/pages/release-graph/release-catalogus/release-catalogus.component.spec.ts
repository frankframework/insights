import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { ReleaseCatalogusComponent } from './release-catalogus.component';

describe('ReleaseCatalogusComponent', () => {
  let component: ReleaseCatalogusComponent;
  let fixture: ComponentFixture<ReleaseCatalogusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseCatalogusComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseCatalogusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Extended Support Display', () => {
    it('should not show extended support timeline by default', () => {
      component.showExtendedSupport = false;
      component.modalOpen = true;
      fixture.detectChanges();

      const extendedSupportElements = fixture.debugElement.queryAll(
        By.css('.support-extended')
      );

      expect(extendedSupportElements.length).toBe(0);
    });

    it('should show extended support timeline when showExtendedSupport is true', () => {
      component.showExtendedSupport = true;
      component.modalOpen = true;
      fixture.detectChanges();

      const extendedSupportElements = fixture.debugElement.queryAll(
        By.css('.support-extended')
      );

      expect(extendedSupportElements.length).toBe(1);
    });

    it('should display correct policy text when extended support is disabled', () => {
      component.showExtendedSupport = false;
      component.modalOpen = true;
      fixture.detectChanges();

      const policyText = fixture.debugElement.query(By.css('.policy-text'));

      expect(policyText.nativeElement.textContent).toContain('one year of security support');
    });

    it('should display extended policy text when extended support is enabled', () => {
      component.showExtendedSupport = true;
      component.modalOpen = true;
      fixture.detectChanges();

      const policyText = fixture.debugElement.query(By.css('.policy-text'));

      expect(policyText.nativeElement.textContent).toContain('18 months total support');
      expect(policyText.nativeElement.textContent).toContain('6 months active + 6 months extended + 6 months security');
    });

    it('should show Extended Support label when enabled', () => {
      component.showExtendedSupport = true;
      component.modalOpen = true;
      fixture.detectChanges();

      const compiled = fixture.nativeElement;

      expect(compiled.textContent).toContain('Extended Support');
      expect(compiled.textContent).toContain('Branch receives extended active development and bug fixes');
    });

    it('should toggle modal visibility', () => {
      expect(component.modalOpen).toBe(false);

      component.toggleModal();

      expect(component.modalOpen).toBe(true);

      component.toggleModal();

      expect(component.modalOpen).toBe(false);
    });
  });
});
