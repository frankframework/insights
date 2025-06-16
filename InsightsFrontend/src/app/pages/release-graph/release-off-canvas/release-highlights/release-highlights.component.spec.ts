import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseHighlightsComponent } from './release-highlights.component';
import { ReleaseOffCanvasComponent } from '../release-off-canvas.component';
import { Issue } from '../../../../services/issue.service';
import { Label } from '../../../../services/label.service';

const mockReleaseOffCanvasComponent = {
  colorNameToRgba: (color: string) => `rgba(${color},0.75)`,
};

const mockIssues: Issue[] = [
  { id: 'i1', number: 1, title: 'Bug A', state: 0, url: '', issueType: { id: 't1', name: 'Bug', color: 'red', description: '' } },
  { id: 'i2', number: 2, title: 'Feature B', state: 0, url: '', issueType: { id: 't2', name: 'Feature', color: 'blue', description: '' } },
  { id: 'i3', number: 3, title: 'Bug C', state: 0, url: '', issueType: { id: 't1', name: 'Bug', color: 'red', description: '' } },
  { id: 'i4', number: 4, title: 'No Type D', state: 0, url: '' }, // Issue with no type
];

describe('ReleaseHighlightsComponent', () => {
  let component: ReleaseHighlightsComponent;
  let fixture: ComponentFixture<ReleaseHighlightsComponent>;
  let parentComponent: ReleaseOffCanvasComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseHighlightsComponent],
      providers: [
        { provide: ReleaseOffCanvasComponent, useValue: mockReleaseOffCanvasComponent },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseHighlightsComponent);
    component = fixture.componentInstance;
    parentComponent = TestBed.inject(ReleaseOffCanvasComponent);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnChanges and Chart Data Generation', () => {
    it('should call generatePieData when releaseIssues input changes', () => {
      spyOn(component as any, 'generatePieData').and.callThrough();
      component.releaseIssues = mockIssues;
      component.ngOnChanges();

      expect((component as any).generatePieData).toHaveBeenCalled();
    });

    it('should correctly aggregate issue types into doughnut chart data', () => {
      spyOn(parentComponent, 'colorNameToRgba').and.callThrough();
      component.releaseIssues = mockIssues;
      component.ngOnChanges();

      const chartData = component.doughnutChartData;
      const dataset = chartData.datasets[0];

      expect(chartData.labels).toEqual(['Bug', 'Feature']);
      expect(dataset.data).toEqual([2, 1]); // 2 bugs, 1 feature
      expect(dataset.backgroundColor).toEqual(['rgba(red,0.75)', 'rgba(blue,0.75)']);
      expect(parentComponent.colorNameToRgba).toHaveBeenCalledWith('red');
      expect(parentComponent.colorNameToRgba).toHaveBeenCalledWith('blue');
    });

    it('should not generate data if releaseIssues is undefined', () => {
      component.releaseIssues = undefined;
      component.ngOnChanges();

      const chartData = component.doughnutChartData;

      expect(chartData.labels?.length).toBe(0);
      expect(chartData.datasets.length).toBe(0);
    });

    it('should ignore issues that have no issueType', () => {
      component.releaseIssues = mockIssues;
      component.ngOnChanges();

      const chartData = component.doughnutChartData;
      expect(chartData.labels).not.toContain('No Type D');
    });
  });

  describe('getDotColor', () => {


      ould return the color as-is if it already starts with #', () => {
      expect(component.getDotColor('#abcdef')).toBe('#abcdef');
    });

    it('should add a # to the color if it is missing', () => {
      expect(component.getDotColor('abcdef')).toBe('#abcdef');
    });
  });
});
