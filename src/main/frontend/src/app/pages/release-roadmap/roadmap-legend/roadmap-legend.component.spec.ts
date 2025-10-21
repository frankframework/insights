import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RoadmapLegend } from './roadmap-legend.component';
import { ISSUE_STATE_STYLES } from '../release-roadmap.component';

describe('RoadmapLegend', () => {
  let component: RoadmapLegend;
  let fixture: ComponentFixture<RoadmapLegend>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoadmapLegend]
    }).compileComponents();

    fixture = TestBed.createComponent(RoadmapLegend);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize issue state items from ISSUE_STATE_STYLES', () => {
    expect(component.issueStateItems.length).toBe(Object.keys(ISSUE_STATE_STYLES).length);

    const labels = component.issueStateItems.map(item => item.label);

    expect(labels).toContain('Todo');
    expect(labels).toContain('On hold');
    expect(labels).toContain('In Progress');
    expect(labels).toContain('Review');
    expect(labels).toContain('Done');
  });

  it('should set correct styles for issue state items', () => {
    const todoItem = component.issueStateItems.find(item => item.label === 'Todo');

    expect(todoItem).toBeDefined();
    expect(todoItem!.style['background-color']).toBe('#f0fdf4');
    expect(todoItem!.style.color).toBe('#166534');
    expect(todoItem!.style['border-color']).toBe('#86efac');
  });

  it('should render legend title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const title = compiled.querySelector('.legend-title');

    expect(title).toBeTruthy();
    expect(title?.textContent?.trim()).toBe('Legend');
  });

  it('should render all issue state items', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const items = compiled.querySelectorAll('.legend-item');

    expect(items.length).toBe(component.issueStateItems.length);
  });

  it('should render color boxes with correct styles', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const colorBoxes = compiled.querySelectorAll('.legend-color-box');

    expect(colorBoxes.length).toBe(component.issueStateItems.length);

    // Check first color box has background color applied
    const firstBox = colorBoxes[0] as HTMLElement;

    expect(firstBox.style.backgroundColor).toBeTruthy();
  });

  it('should render labels for each item', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const labels = compiled.querySelectorAll('.legend-label');

    expect(labels.length).toBe(component.issueStateItems.length);

    const labelTexts = [...labels].map(label => label.textContent?.trim());

    expect(labelTexts).toContain('Todo');
    expect(labelTexts).toContain('On hold');
    expect(labelTexts).toContain('In Progress');
    expect(labelTexts).toContain('Review');
    expect(labelTexts).toContain('Done');
  });

  it('should render legend in a column layout', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const column = compiled.querySelector('.legend-column');

    expect(column).toBeTruthy();
  });

  it('should have correct CSS classes for styling', () => {
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('.roadmap-legend')).toBeTruthy();
    expect(compiled.querySelector('.legend-content')).toBeTruthy();
    expect(compiled.querySelector('.legend-title')).toBeTruthy();
    expect(compiled.querySelector('.legend-column')).toBeTruthy();
  });
});
