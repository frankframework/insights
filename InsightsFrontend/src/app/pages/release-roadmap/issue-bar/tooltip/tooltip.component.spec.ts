import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BehaviorSubject } from 'rxjs';

import { TooltipComponent } from './tooltip.component';
import { TooltipData, TooltipService } from './tooltip.service';
import { GitHubStates } from '../../../../app.service';
import { Issue } from '../../../../services/issue.service';

const MOCK_ISSUE: Issue = {
  id: '1',
  number: 123,
  title: 'Tooltip Test Issue',
  url: 'http://example.com',
  state: GitHubStates.OPEN,
  points: 8,
  issuePriority: { id: 'p1', name: 'High', color: 'ff0000', description: '' },
};

describe('TooltipService', () => {
  let service: TooltipService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TooltipService],
    });
    service = TestBed.inject(TooltipService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should emit tooltip data on show()', (done) => {
    const mockElement = document.createElement('div');

    spyOn(mockElement, 'getBoundingClientRect').and.returnValue({
      top: 100,
      left: 200,
      width: 50,
      height: 20,
    } as DOMRect);

    service.tooltipState$.subscribe((data) => {
      if (data) {
        expect(data.issue).toBe(MOCK_ISSUE);
        expect(data.top).toBe('92px');
        expect(data.left).toBe('225px');
        done();
      }
    });

    service.show(mockElement, MOCK_ISSUE);
  });

  it('should emit null on hide()', (done) => {

    service.show(document.createElement('div'), MOCK_ISSUE);

    service.tooltipState$.subscribe((data) => {
      if (data === null) {
        expect(data).toBeNull();
        done();
      }
    });

    service.hide();
  });
});

describe('TooltipComponent', () => {
  let component: TooltipComponent;
  let fixture: ComponentFixture<TooltipComponent>;
  let tooltipSubject: BehaviorSubject<TooltipData | null>;

  beforeEach(async () => {
    tooltipSubject = new BehaviorSubject<TooltipData | null>(null);

    await TestBed.configureTestingModule({
      imports: [TooltipComponent],
      providers: [
        { provide: TooltipService, useValue: { tooltipState$: tooltipSubject.asObservable() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TooltipComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should not display the tooltip when service state is null', () => {
    tooltipSubject.next(null);
    fixture.detectChanges();
    const tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).toBeNull();
  });

  it('should display the tooltip with correct data when service emits state', fakeAsync(() => {
    const tooltipData: TooltipData = {
      issue: MOCK_ISSUE,
      top: '100px',
      left: '200px',
    };
    tooltipSubject.next(tooltipData);
    fixture.detectChanges();
    tick();

    const tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).not.toBeNull();

    const titleElement = tooltipElement.query(By.css('.tooltip-title')).nativeElement;
    const detailsElement = tooltipElement.queryAll(By.css('.tooltip-detail'));

    expect(titleElement.textContent).toContain(MOCK_ISSUE.title);
    expect(detailsElement.length).toBe(2);
    expect(detailsElement[0].nativeElement.textContent).toContain('Priority: High');
    expect(detailsElement[1].nativeElement.textContent).toContain('Points: 8');
  }));

  it('should hide the tooltip when service emits null after showing', fakeAsync(() => {
    const tooltipData: TooltipData = { issue: MOCK_ISSUE, top: '100px', left: '200px' };
    tooltipSubject.next(tooltipData);
    fixture.detectChanges();
    tick();

    let tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).not.toBeNull();

    tooltipSubject.next(null);
    fixture.detectChanges();
    tick();

    tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).toBeNull();
  }));
});
