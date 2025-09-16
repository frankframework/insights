import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';
import { ReleaseOffCanvasComponent } from './release-off-canvas.component';
import { LabelService, Label } from '../../../services/label.service';
import { IssueService, Issue } from '../../../services/issue.service';
import { ToastrService } from 'ngx-toastr';
import { Release } from '../../../services/release.service';
import { GitHubStates } from '../../../app.service';

const mockLabelService = jasmine.createSpyObj('LabelService', ['getHighLightsByReleaseId']);
const mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
const mockToastrService = jasmine.createSpyObj('ToastrService', ['error']);

const mockRelease: Release = {
  id: 'release-1',
  name: 'v1.0.0',
  tagName: 'v1',
  publishedAt: new Date(),
  branch: { id: 'b1', name: 'master' },
};
const mockLabels: Label[] = [{ id: 'label-1', name: 'Highlight', color: '0000ff', description: '' }];
const mockIssues: Issue[] = [{ id: 'issue-1', number: 123, title: 'Test Issue', state: GitHubStates.OPEN, url: '' }];

describe('ReleaseOffCanvasComponent', () => {
  let component: ReleaseOffCanvasComponent;
  let fixture: ComponentFixture<ReleaseOffCanvasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseOffCanvasComponent],
      providers: [
        { provide: LabelService, useValue: mockLabelService },
        { provide: IssueService, useValue: mockIssueService },
        { provide: ToastrService, useValue: mockToastrService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseOffCanvasComponent);
    component = fixture.componentInstance;

    mockLabelService.getHighLightsByReleaseId.calls.reset();
    mockIssueService.getIssuesByReleaseId.calls.reset();
    mockToastrService.error.calls.reset();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnChanges - Data Fetching', () => {
    const triggerNgOnChanges = () => {
      component.release = mockRelease;
      component.ngOnChanges({ release: new SimpleChange(null, mockRelease, true) });
    };

    it('should set isLoading to true initially, and then to false after data is fetched', fakeAsync(() => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));

      triggerNgOnChanges();

      expect(component.isLoading).toBe(true);

      tick();

      expect(component.isLoading).toBe(false);
    }));

    it('should fetch data when a valid release is provided', fakeAsync(() => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));

      triggerNgOnChanges();
      tick();

      expect(mockLabelService.getHighLightsByReleaseId).toHaveBeenCalledWith('release-1');
      expect(mockIssueService.getIssuesByReleaseId).toHaveBeenCalledWith('release-1');
      expect(component.highlightedLabels).toEqual(mockLabels);
      expect(component.releaseIssues).toEqual(mockIssues);
    }));

    it('should not fetch data if release is null or id is missing', () => {
      component.release = null as any;
      component.ngOnChanges({ release: new SimpleChange(null, null, true) });

      expect(mockLabelService.getHighLightsByReleaseId).not.toHaveBeenCalled();
      expect(mockIssueService.getIssuesByReleaseId).not.toHaveBeenCalled();
    });

    it('should handle label fetch error gracefully', fakeAsync(() => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(
        throwError(() => new Error('Label API Error')).pipe(delay(0)),
      );
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues).pipe(delay(0)));

      triggerNgOnChanges();
      tick();

      expect(component.isLoading).toBe(false);
      expect(mockToastrService.error).toHaveBeenCalledWith(
        'Failed to load release highlights. Please try again later.',
      );
      expect(component.highlightedLabels).toBeUndefined();
      expect(component.releaseIssues).toBeUndefined();
    }));

    it('should handle issue fetch error gracefully', fakeAsync(() => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(
        throwError(() => new Error('Issue API Error')).pipe(delay(0)),
      );

      triggerNgOnChanges();
      tick();

      expect(component.isLoading).toBe(false);
      expect(mockToastrService.error).toHaveBeenCalledWith('Failed to load release issues. Please try again later.');
      expect(component.releaseIssues).toBeUndefined();
      expect(component.highlightedLabels).toBeUndefined();
    }));

    it('should show a toast and set data to undefined if API returns an empty array', fakeAsync(() => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]).pipe(delay(0)));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of([]).pipe(delay(0)));

      // Act
      triggerNgOnChanges();
      tick();

      expect(component.highlightedLabels).toBeUndefined();
      expect(component.releaseIssues).toBeUndefined();
      expect(mockToastrService.error).toHaveBeenCalledWith('No release highlights found.');
      expect(mockToastrService.error).toHaveBeenCalledWith('No release issues found.');
    }));
  });

  describe('Outputs', () => {
    it('should emit closeCanvas event', () => {
      spyOn(component.closeCanvas, 'emit');

      component.closeCanvas.emit();

      expect(component.closeCanvas.emit).toHaveBeenCalledWith();
    });
  });
});
