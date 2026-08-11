import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ComponentRef } from '@angular/core';
import { StatCardComponent } from './stat-card.component';

describe('StatCardComponent', () => {
  let component: StatCardComponent;
  let fixture: ComponentFixture<StatCardComponent>;
  let componentReference: ComponentRef<StatCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatCardComponent);
    component = fixture.componentInstance;
    componentReference = fixture.componentRef;
  });

  it('should create', () => {
    componentReference.setInput('label', 'CVSS');
    componentReference.setInput('value', 7.5);
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('renders the label and value', () => {
    componentReference.setInput('label', 'CVSS');
    componentReference.setInput('value', 7.5);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.stat-label').textContent.trim()).toBe('CVSS');
    expect(fixture.nativeElement.querySelector('.stat-value').textContent.trim()).toBe('7.5');
  });

  it('is a neutral card when no badge class is given', () => {
    componentReference.setInput('label', 'Published');
    componentReference.setInput('value', 'Jan 15, 2026');
    fixture.detectChanges();

    expect(fixture.nativeElement.classList).toContain('stat-card');
    expect(fixture.nativeElement.classList).not.toContain('priority-card');
  });

  it('tints as a priority card when a badge class is given', () => {
    componentReference.setInput('label', 'Severity');
    componentReference.setInput('value', 'High');
    componentReference.setInput('badgeClass', 'badge-high');
    fixture.detectChanges();

    const { classList } = fixture.nativeElement;

    expect(classList).toContain('stat-card');
    expect(classList).toContain('priority-card');
    expect(classList).toContain('badge-high');
  });
});
