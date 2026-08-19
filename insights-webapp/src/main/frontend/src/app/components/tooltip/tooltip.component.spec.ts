import { WritableSignal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { TooltipComponent } from './tooltip.component';
import { TooltipData, TooltipService } from './tooltip.service';

describe('TooltipService', () => {
  let service: TooltipService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(TooltipService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should expose tooltip data on show()', () => {
    const mockElement = document.createElement('div');

    spyOn(mockElement, 'getBoundingClientRect').and.returnValue({
      top: 100,
      left: 200,
      width: 50,
      height: 20,
    } as DOMRect);

    service.show(mockElement, 'Test Title', [{ label: 'Priority', value: 'High' }]);

    expect(service.tooltip()).toEqual({
      title: 'Test Title',
      details: [{ label: 'Priority', value: 'High' }],
      top: '92px',
      left: '225px',
    });
  });

  it('should default to an empty details array', () => {
    service.show(document.createElement('div'), 'Test Title');

    expect(service.tooltip()?.details).toEqual([]);
  });

  it('should expose null on hide()', () => {
    service.show(document.createElement('div'), 'Test Title');

    service.hide();

    expect(service.tooltip()).toBeNull();
  });
});

describe('TooltipComponent', () => {
  let component: TooltipComponent;
  let fixture: ComponentFixture<TooltipComponent>;
  let tooltipState: WritableSignal<TooltipData | null>;

  beforeEach(async () => {
    tooltipState = signal<TooltipData | null>(null);

    await TestBed.configureTestingModule({
      imports: [TooltipComponent],
      providers: [{ provide: TooltipService, useValue: { tooltip: tooltipState.asReadonly() } }],
    }).compileComponents();

    fixture = TestBed.createComponent(TooltipComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should not display the tooltip when service state is null', () => {
    tooltipState.set(null);
    fixture.detectChanges();
    const tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).toBeNull();
  });

  it('should display the tooltip with correct data when service emits state', () => {
    const tooltipData: TooltipData = {
      title: 'Tooltip Test Issue',
      details: [
        { label: 'Priority', value: 'High' },
        { label: 'Points', value: '8' },
      ],
      top: '100px',
      left: '200px',
    };
    tooltipState.set(tooltipData);
    fixture.detectChanges();

    const tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).not.toBeNull();

    const titleElement = tooltipElement.query(By.css('.tooltip-title')).nativeElement;
    const detailsElement = tooltipElement.queryAll(By.css('.tooltip-detail'));

    expect(titleElement.textContent).toContain(tooltipData.title);
    expect(detailsElement.length).toBe(2);
    expect(detailsElement[0].nativeElement.textContent).toContain('Priority: High');
    expect(detailsElement[1].nativeElement.textContent).toContain('Points: 8');
  });

  it('should render a detail without a label as plain text', () => {
    const tooltipData: TooltipData = {
      title: 'Attack Vector',
      details: [{ value: 'How the vulnerability is exploited' }],
      top: '100px',
      left: '200px',
    };
    tooltipState.set(tooltipData);
    fixture.detectChanges();

    const detailElement = fixture.debugElement.query(By.css('.tooltip-detail'));

    expect(detailElement.nativeElement.textContent.trim()).toBe('How the vulnerability is exploited');
  });

  it('should hide the tooltip when service emits null after showing', () => {
    const tooltipData: TooltipData = { title: 'Tooltip Test Issue', details: [], top: '100px', left: '200px' };
    tooltipState.set(tooltipData);
    fixture.detectChanges();

    let tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).not.toBeNull();

    tooltipState.set(null);
    fixture.detectChanges();

    tooltipElement = fixture.debugElement.query(By.css('.tooltip'));

    expect(tooltipElement).toBeNull();
  });
});
