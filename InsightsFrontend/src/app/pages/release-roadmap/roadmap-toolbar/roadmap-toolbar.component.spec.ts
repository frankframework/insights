import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RoadmapToolbarComponent } from './roadmap-toolbar.component';

describe('RoadmapToolbarComponent', () => {
  let component: RoadmapToolbarComponent;
  let fixture: ComponentFixture<RoadmapToolbarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoadmapToolbarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RoadmapToolbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Input: periodLabel', () => {
    it('should display the periodLabel when it is set', () => {
      const testLabel = 'Q3 2025 - Q4 2025';
      component.periodLabel = testLabel;
      fixture.detectChanges();
      const label = fixture.debugElement.query(By.css('.period-label')).nativeElement;

      expect(label.textContent?.trim()).toBe(testLabel);
    });

    it('should display an empty string if periodLabel is not set', () => {
      component.periodLabel = '';
      fixture.detectChanges();
      const label = fixture.debugElement.query(By.css('.period-label')).nativeElement;

      expect(label.textContent?.trim()).toBe('');
    });
  });

  describe('Output: Event Emitters', () => {
    it('should emit changePeriod with -3 when the previous button is clicked', () => {
      spyOn(component.changePeriod, 'emit');
      const previousButton = fixture.debugElement.query(By.css('button[title="Previous quarter"]'));

      previousButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.changePeriod.emit).toHaveBeenCalledWith(-3);
      expect(component.changePeriod.emit).toHaveBeenCalledTimes(1);
    });

    it('should emit changePeriod with 3 when the next button is clicked', () => {
      spyOn(component.changePeriod, 'emit');
      const nextButton = fixture.debugElement.query(By.css('button[title="Next quarter"]'));

      nextButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.changePeriod.emit).toHaveBeenCalledWith(3);
      expect(component.changePeriod.emit).toHaveBeenCalledTimes(1);
    });

    it('should emit resetPeriod when the today button is clicked', () => {
      spyOn(component.resetPeriod, 'emit');
      const todayButton = fixture.debugElement.query(By.css('button[title="Go to today"]'));

      todayButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.resetPeriod.emit).toHaveBeenCalledWith();
      expect(component.resetPeriod.emit).toHaveBeenCalledTimes(1);
    });
  });
});
