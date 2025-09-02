import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseHighlightsComponent } from './release-highlights.component';
import { Issue } from '../../../../services/issue.service';
import { GitHubStates } from '../../../../app.service';
import { ColorService } from '../../../../services/color.service';

const mockIssues: Issue[] = [
  { id: 'i1', number: 1, title: 'Bug A', state: GitHubStates.OPEN, url: '', issueType: { id: 'id-1', name: 'Bug', color: 'red', description: 'description-1' } },
  { id: 'i2', number: 2, title: 'Feature B', state: GitHubStates.OPEN, url: '', issueType: { id: 'id-2', name: 'Feature', color: 'blue', description: 'description-2' } },
  { id: 'i3', number: 3, title: 'Bug C', state: GitHubStates.OPEN, url: '', issueType: { id: 'id-3', name: 'Bug', color: 'red', description: 'description-3' } },
  { id: 'i4', number: 4, title: 'No Type D', state: GitHubStates.OPEN, url: '' },
];

describe('ReleaseHighlightsComponent', () => {
  let component: ReleaseHighlightsComponent;
  let fixture: ComponentFixture<ReleaseHighlightsComponent>;
  let colorService: ColorService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseHighlightsComponent],
      providers: [
        ColorService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseHighlightsComponent);
    component = fixture.componentInstance;
    colorService = TestBed.inject(ColorService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnChanges and Chart Data Generation', () => {
    it('should call generatePieData when releaseIssues input changes', () => {
      spyOn(component as any, 'generatePieData').and.callThrough();
      component.releaseIssues = mockIssues;
      component.ngOnChanges();

      expect((component as any).generatePieData).toHaveBeenCalledWith();
    });

    it('should correctly aggregate issue types into doughnut chart data', () => {
      spyOn(colorService, 'colorNameToRgba').and.callFake((color: string) => `rgba(${color},0.75)`);

      component.releaseIssues = mockIssues;
      component.ngOnChanges();

      const chartData = component.doughnutChartData;
      const dataset = chartData.datasets[0];

      expect(chartData.labels).toEqual(['Bug', 'Feature']);
      expect(dataset.data).toEqual([2, 1]);

      expect(dataset.backgroundColor).toEqual(['rgba(red,0.75)', 'rgba(blue,0.75)']);
      expect(colorService.colorNameToRgba).toHaveBeenCalledWith('red');
      expect(colorService.colorNameToRgba).toHaveBeenCalledWith('blue');
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

      // Zorg ervoor dat de data van "No Type D" niet is meegenomen
      expect(chartData.labels).not.toContain('No Type D');
      expect(chartData.labels?.length).toBe(2);
    });
  });

  describe('getDotColor', () => {
    it('should return the color as-is if it already starts with #', () => {
      expect(component.getDotColor('#abcdef')).toBe('#abcdef');
    });

    it('should add a # to the color if it is missing', () => {
      expect(component.getDotColor('abcdef')).toBe('#abcdef');
    });
  });
});
