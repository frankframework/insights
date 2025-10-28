import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RoadmapToolbarComponent } from './roadmap-toolbar.component';
import { ViewMode } from '../release-roadmap.component';

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
    it('should emit changePeriod with -3 when the previous button is clicked in quarterly mode', () => {
      component.viewMode = ViewMode.QUARTERLY;
      fixture.detectChanges();

      spyOn(component.changePeriod, 'emit');
      const previousButton = fixture.debugElement.query(By.css('button[title="Previous quarter"]'));

      previousButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.changePeriod.emit).toHaveBeenCalledWith(-3);
      expect(component.changePeriod.emit).toHaveBeenCalledTimes(1);
    });

    it('should emit changePeriod with 3 when the next button is clicked in quarterly mode', () => {
      component.viewMode = ViewMode.QUARTERLY;
      fixture.detectChanges();

      spyOn(component.changePeriod, 'emit');
      const nextButton = fixture.debugElement.query(By.css('button[title="Next quarter"]'));

      nextButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.changePeriod.emit).toHaveBeenCalledWith(3);
      expect(component.changePeriod.emit).toHaveBeenCalledTimes(1);
    });

    it('should emit changePeriod with -1 when the previous button is clicked in monthly mode', () => {
      component.viewMode = ViewMode.MONTHLY;
      fixture.detectChanges();

      spyOn(component.changePeriod, 'emit');
      const previousButton = fixture.debugElement.query(By.css('button[title="Previous month"]'));

      previousButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.changePeriod.emit).toHaveBeenCalledWith(-1);
    });

    it('should emit changePeriod with 1 when the next button is clicked in monthly mode', () => {
      component.viewMode = ViewMode.MONTHLY;
      fixture.detectChanges();

      spyOn(component.changePeriod, 'emit');
      const nextButton = fixture.debugElement.query(By.css('button[title="Next month"]'));

      nextButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.changePeriod.emit).toHaveBeenCalledWith(1);
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

  describe('ViewMode Toggle', () => {
    it('should emit toggleViewMode when switching from quarterly to monthly', () => {
      component.viewMode = ViewMode.QUARTERLY;
      fixture.detectChanges();

      spyOn(component.toggleViewMode, 'emit');

      const monthlyButton = fixture.debugElement.queryAll(By.css('.toggle-button'))[1];

      monthlyButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.toggleViewMode.emit).toHaveBeenCalledWith();
      expect(component.toggleViewMode.emit).toHaveBeenCalledTimes(1);
    });

    it('should emit toggleViewMode when switching from monthly to quarterly', () => {
      component.viewMode = ViewMode.MONTHLY;
      fixture.detectChanges();

      spyOn(component.toggleViewMode, 'emit');

      const quarterlyButton = fixture.debugElement.queryAll(By.css('.toggle-button'))[0];

      quarterlyButton.triggerEventHandler('click', null);
      fixture.detectChanges();

      expect(component.toggleViewMode.emit).toHaveBeenCalledWith();
      expect(component.toggleViewMode.emit).toHaveBeenCalledTimes(1);
    });

    it('should apply an "active" class to the correct view mode button', () => {
      component.viewMode = ViewMode.QUARTERLY;
      fixture.detectChanges();

      const toggleButtons = fixture.debugElement.queryAll(By.css('.toggle-button'));
      const quarterlyButton = toggleButtons[0];
      const monthlyButton = toggleButtons[1];

      expect(quarterlyButton.classes['active']).toBeTrue();
      expect(monthlyButton.classes['active']).toBeFalsy();

      component.viewMode = ViewMode.MONTHLY;
      fixture.detectChanges();

      expect(quarterlyButton.classes['active']).toBeFalsy();
      expect(monthlyButton.classes['active']).toBeTrue();
    });
  });
});
