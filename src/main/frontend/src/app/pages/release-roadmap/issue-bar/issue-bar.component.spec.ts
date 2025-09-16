import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { IssueBarComponent } from './issue-bar.component';
import { Issue, IssuePriority } from '../../../services/issue.service';
import { GitHubStates } from '../../../app.service';
import { TooltipService } from './tooltip/tooltip.service';

const MOCK_ISSUE: Issue = {
  id: '1',
  number: 123,
  title: 'Test Issue Title',
  url: 'https://github.com/test/issue/123',
  state: GitHubStates.OPEN,
  points: 5,
};

class MockTooltipService {
  show = jasmine.createSpy('show');
  hide = jasmine.createSpy('hide');
}

describe('IssueBarComponent', () => {
  let component: IssueBarComponent;
  let fixture: ComponentFixture<IssueBarComponent>;
  let tooltipService: MockTooltipService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueBarComponent],
      providers: [{ provide: TooltipService, useClass: MockTooltipService }],
    }).compileComponents();

    fixture = TestBed.createComponent(IssueBarComponent);
    component = fixture.componentInstance;
    tooltipService = TestBed.inject(TooltipService) as unknown as MockTooltipService;

    component.issue = MOCK_ISSUE;
  });

  it('should create', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  describe('Styling Logic', () => {
    it('should apply CLOSED_STYLE for a closed issue', () => {
      component.issue = { ...MOCK_ISSUE, state: GitHubStates.CLOSED };
      fixture.detectChanges();
      const closedStyle = (component as any).CLOSED_STYLE;

      expect(component.priorityStyle).toEqual(closedStyle);
    });

    it('should apply OPEN_STYLE for an open issue without a priority color', () => {
      component.issue = { ...MOCK_ISSUE, issuePriority: undefined };
      fixture.detectChanges();
      const openStyle = (component as any).OPEN_STYLE;

      expect(component.priorityStyle).toEqual(openStyle);
    });

    it('should apply OPEN_STYLE for an open issue with an invalid priority color', () => {
      const priorityWithInvalidColor: IssuePriority = { id: 'p1', name: 'High', color: 'invalid', description: '' };
      component.issue = { ...MOCK_ISSUE, issuePriority: priorityWithInvalidColor };
      fixture.detectChanges();
      const openStyle = (component as any).OPEN_STYLE;

      expect(component.priorityStyle).toEqual(openStyle);
    });

    it('should generate custom styles for an open issue with a valid priority color', () => {
      const priorityWithValidColor: IssuePriority = { id: 'p1', name: 'High', color: 'ff0000', description: '' };
      component.issue = { ...MOCK_ISSUE, issuePriority: priorityWithValidColor };
      fixture.detectChanges();

      const expectedStyle = {
        'background-color': '#ff000025',
        color: '#ff0000',
        'border-color': '#ff0000',
      };

      expect(component.priorityStyle).toEqual(expectedStyle);
    });
  });

  describe('Tooltip Interaction', () => {
    it('should call TooltipService.show on mouseenter', () => {
      fixture.detectChanges();
      const issueLink = fixture.debugElement.query(By.css('.issue-bar'));
      issueLink.triggerEventHandler('mouseenter', null);

      expect(tooltipService.show).toHaveBeenCalledWith(issueLink.nativeElement, component.issue);
    });

    it('should call TooltipService.hide on mouseleave', () => {
      fixture.detectChanges();
      const issueLink = fixture.debugElement.query(By.css('.issue-bar'));
      issueLink.triggerEventHandler('mouseleave', null);

      expect(tooltipService.hide).toHaveBeenCalledWith();
    });
  });

  describe('Template Rendering', () => {
    it('should render the issue number', () => {
      fixture.detectChanges();
      const numberElement = fixture.debugElement.query(By.css('.number')).nativeElement;

      expect(numberElement.textContent).toContain('123');
    });

    it('should set the href attribute on the anchor tag', () => {
      fixture.detectChanges();
      const anchorElement = fixture.debugElement.query(By.css('.issue-bar')).nativeElement as HTMLAnchorElement;

      expect(anchorElement.href).toBe(MOCK_ISSUE.url);
    });

    it('should apply the calculated position style to the host element', () => {
      component.issueStyle = { left: '10%', width: '5%' };
      fixture.detectChanges();
      const anchorElement = fixture.debugElement.query(By.css('.issue-bar')).nativeElement as HTMLAnchorElement;

      expect(anchorElement.style.left).toBe('10%');
      expect(anchorElement.style.width).toBe('5%');
    });
  });
});
