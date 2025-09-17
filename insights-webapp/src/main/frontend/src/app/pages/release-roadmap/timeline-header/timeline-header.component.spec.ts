import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DatePipe } from '@angular/common';
import { TimelineHeaderComponent } from './timeline-header.component';

describe('TimelineHeaderComponent', () => {
  let component: TimelineHeaderComponent;
  let fixture: ComponentFixture<TimelineHeaderComponent>;
  let nativeElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimelineHeaderComponent],
      providers: [DatePipe],
    }).compileComponents();

    fixture = TestBed.createComponent(TimelineHeaderComponent);
    component = fixture.componentInstance;
    nativeElement = fixture.nativeElement;
  });

  it('should create', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  describe('Quarters Display', () => {
    const mockQuarters = [
      { name: 'Q2 2025', monthCount: 3 },
      { name: 'Q3 2025', monthCount: 3 },
    ];

    it('should render the correct number of quarter cells', () => {
      component.quarters = mockQuarters;
      fixture.detectChanges();
      const quarterCells = nativeElement.querySelectorAll('.quarter-cell');

      expect(quarterCells.length).toBe(2);
    });

    it('should display the quarter names correctly', () => {
      component.quarters = mockQuarters;
      fixture.detectChanges();
      const quarterCells = nativeElement.querySelectorAll('.quarter-cell');

      expect(quarterCells[0].textContent).toContain('Q2 2025');
      expect(quarterCells[1].textContent).toContain('Q3 2025');
    });

    it('should calculate quartersGridStyle correctly based on monthCount', () => {
      component.quarters = mockQuarters;

      expect(component.quartersGridStyle).toBe('3fr 3fr');
    });

    it('should apply the calculated grid style to the quarters grid area', () => {
      component.quarters = mockQuarters;
      fixture.detectChanges();
      const quartersGridArea = nativeElement.querySelector('.quarters-row .grid-area') as HTMLElement;

      expect(quartersGridArea.style.gridTemplateColumns).toBe('3fr 3fr');
    });
  });

  describe('Months Display', () => {
    const mockMonths = [new Date('2025-04-01'), new Date('2025-05-01'), new Date('2025-06-01')];

    it('should render the correct number of month cells', () => {
      component.months = mockMonths;
      fixture.detectChanges();
      const monthCells = nativeElement.querySelectorAll('.month-cell');

      expect(monthCells.length).toBe(3);
    });

    it('should display the abbreviated month names correctly using the DatePipe', () => {
      component.months = mockMonths;
      fixture.detectChanges();
      const monthCells = nativeElement.querySelectorAll('.month-cell');

      expect(monthCells[0].textContent).toContain('Apr');
      expect(monthCells[1].textContent).toContain('May');
      expect(monthCells[2].textContent).toContain('Jun');
    });

    it('should apply the correct grid style to the months grid area', () => {
      component.months = mockMonths;
      fixture.detectChanges();
      const monthsGridArea = nativeElement.querySelector('.months-row .grid-area') as HTMLElement;

      expect(monthsGridArea.style.gridTemplateColumns).toBe('repeat(3, minmax(0px, 1fr))');
    });
  });
});
