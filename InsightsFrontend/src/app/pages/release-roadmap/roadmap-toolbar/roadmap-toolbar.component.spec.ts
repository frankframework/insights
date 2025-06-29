import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RoadmapToolbarComponent } from './roadmap-toolbar.component';

describe('RoadmapToolbarComponent', () => {
  let component: RoadmapToolbarComponent;
  let fixture: ComponentFixture<RoadmapToolbarComponent>;
  let nativeElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoadmapToolbarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RoadmapToolbarComponent);
    component = fixture.componentInstance;
    nativeElement = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Input: periodLabel', () => {
    it('should display the initial empty periodLabel', () => {
      const label = nativeElement.querySelector('.period-label');
      expect(label?.textContent?.trim()).toBe('');
    });

    it('should display the periodLabel when it is set', () => {
      const testLabel = 'Q3 2025 - Q4 2025';
      component.periodLabel = testLabel;
      fixture.detectChanges();
      const label = nativeElement.querySelector('.period-label');
      expect(label?.textContent?.trim()).toBe(testLabel);
    });
  });

  describe('Output: Event Emitters', () => {
    it('should emit changePeriod with -3 when the previous button is clicked', () => {
      spyOn(component.changePeriod, 'emit');

      const previousButton = nativeElement.querySelector<HTMLButtonElement>('button[title="Previous quarter"]');
      previousButton?.click();
      fixture.detectChanges();

      expect(component.changePeriod.emit).toHaveBeenCalledWith(-3);
    });

    it('should emit changePeriod with 3 when the next button is clicked', () => {
      spyOn(component.changePeriod, 'emit');
      const nextButton = nativeElement.querySelector<HTMLButtonElement>('button[title="Next quarter"]');
      nextButton?.click();
      fixture.detectChanges();
      expect(component.changePeriod.emit).toHaveBeenCalledWith(3);
    });

    it('should emit resetPeriod when the today button is clicked', () => {
      spyOn(component.resetPeriod, 'emit');
      const todayButton = nativeElement.querySelector<HTMLButtonElement>('button[title="Go to today"]');
      todayButton?.click();
      fixture.detectChanges();
      expect(component.resetPeriod.emit).toHaveBeenCalled();
    });
  });
});
