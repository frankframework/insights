import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ReleaseIncludeControlComponent } from './release-include-control.component';
import { parseIncludeRanges } from '../../../pipes/release-include';

describe('ReleaseIncludeControlComponent', () => {
  let component: ReleaseIncludeControlComponent;
  let fixture: ComponentFixture<ReleaseIncludeControlComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseIncludeControlComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseIncludeControlComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render nothing without included releases', () => {
    expect(component.isActive).toBeFalse();
    expect(fixture.debugElement.query(By.css('.include-chip'))).toBeNull();
  });

  it('should show the active releases', () => {
    component.includedReleases = parseIncludeRanges('7.0-9.3.2');
    fixture.detectChanges();

    expect(component.isActive).toBeTrue();
    expect(component.label).toBe('7.0-9.3.2');
    expect(fixture.debugElement.query(By.css('.include-chip')).nativeElement.textContent).toContain('7.0-9.3.2');
  });

  it('should emit cleared when the clear button is pressed', () => {
    spyOn(component.cleared, 'emit');
    component.includedReleases = parseIncludeRanges('7.1');
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.include-chip-clear')).nativeElement.click();

    expect(component.cleared.emit).toHaveBeenCalledWith();
  });
});
