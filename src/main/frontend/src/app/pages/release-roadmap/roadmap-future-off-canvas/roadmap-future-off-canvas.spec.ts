import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { RoadmapFutureOffCanvasComponent } from './roadmap-future-off-canvas';
import { Issue, IssueService } from '../../../services/issue.service';
import { By } from '@angular/platform-browser';
import { Component, Input } from '@angular/core';
import { GitHubStates } from '../../../app.service';

@Component({ selector: 'app-loader', template: '', standalone: true })
class MockLoaderComponent {}

@Component({ selector: 'app-off-canvas', template: '<ng-content></ng-content>', standalone: true })
class MockOffCanvasComponent {}

@Component({ selector: 'app-future-epic', template: '', standalone: true })
class MockFutureEpicComponent {
  @Input() futureEpicIssue!: Issue;
}

const MOCK_ISSUES: Issue[] = [
  { id: '1', number: 101, title: 'Epic Plan A', url: '', state: GitHubStates.OPEN },
  { id: '2', number: 102, title: 'Epic Plan B', url: '', state: GitHubStates.OPEN },
];

describe('RoadmapFutureOffCanvasComponent', () => {
  let component: RoadmapFutureOffCanvasComponent;
  let fixture: ComponentFixture<RoadmapFutureOffCanvasComponent>;
  let mockIssueService: jasmine.SpyObj<IssueService>;

  beforeEach(async () => {
    mockIssueService = jasmine.createSpyObj('IssueService', ['getFutureEpicIssues']);

    await TestBed.configureTestingModule({
      imports: [RoadmapFutureOffCanvasComponent],
      providers: [{ provide: IssueService, useValue: mockIssueService }],
    })
      .overrideComponent(RoadmapFutureOffCanvasComponent, {
        set: {
          imports: [MockLoaderComponent, MockOffCanvasComponent, MockFutureEpicComponent],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(RoadmapFutureOffCanvasComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    mockIssueService.getFutureEpicIssues.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('should show loader initially and hide it after data is fetched', fakeAsync(() => {
    const issuesSubject = new Subject<Issue[]>();
    mockIssueService.getFutureEpicIssues.and.returnValue(issuesSubject.asObservable());

    fixture.detectChanges();

    let loader = fixture.debugElement.query(By.css('app-loader'));

    expect(loader).withContext('Loader moet zichtbaar zijn tijdens het laden').toBeTruthy();

    issuesSubject.next(MOCK_ISSUES);
    issuesSubject.complete();
    tick();
    fixture.detectChanges();

    loader = fixture.debugElement.query(By.css('app-loader'));

    expect(loader).withContext('Loader moet verborgen zijn na het laden').toBeFalsy();
  }));

  it('should fetch and display future epic issues on init', fakeAsync(() => {
    mockIssueService.getFutureEpicIssues.and.returnValue(of(MOCK_ISSUES));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const epics = fixture.debugElement.queryAll(By.css('app-future-epic'));

    expect(component.isLoading).toBeFalse();
    expect(component.futureEpicIssues).toEqual(MOCK_ISSUES);
    expect(epics.length).toBe(2);
  }));

  it('should show "no plans" message if no issues are returned', fakeAsync(() => {
    mockIssueService.getFutureEpicIssues.and.returnValue(of([]));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const noPlansMessage = fixture.debugElement.query(By.css('.no-new-features'));

    expect(component.isLoading).toBeFalse();
    expect(component.futureEpicIssues).toBeUndefined();
    expect(noPlansMessage).toBeTruthy();
    expect(noPlansMessage.nativeElement.textContent).toContain('No future plans found.');
  }));

  it('should emit closeCanvas event when close is triggered from child component', () => {
    spyOn(component.closeCanvas, 'emit');
    mockIssueService.getFutureEpicIssues.and.returnValue(of([]));
    fixture.detectChanges();

    const offCanvasComponent = fixture.debugElement.query(By.css('app-off-canvas'));
    offCanvasComponent.triggerEventHandler('closeCanvas', null);

    expect(component.closeCanvas.emit).toHaveBeenCalledWith();
  });
});
