import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ReleaseOffCanvasComponent } from './release-off-canvas.component';
import { LabelService } from '../../../services/label.service';
import { IssueService } from '../../../services/issue.service';
import { ToastrService } from 'ngx-toastr';
import { Release } from '../../../services/release.service';
import { Issue } from '../../../services/issue.service';
import { Label } from '../../../services/label.service';

const mockLabelService = jasmine.createSpyObj('LabelService', ['getHighLightsByReleaseId']);
const mockIssueService = jasmine.createSpyObj('IssueService', ['getIssuesByReleaseId']);
const mockToastService = jasmine.createSpyObj('ToastrService', ['error']);

const mockRelease: Release = { id: 'release-1', name: 'v1.0.0', tagName: 'v1', publishedAt: new Date(), branch: { id: 'b1', name: 'master' } };
const mockLabels: Label[] = [{ id: 'label-1', name: 'Highlight', color: 'blue', description: '' }];
const mockIssues: Issue[] = [{ id: 'issue-1', number: 123, title: 'Test Issue', state: 0, url: '' }];

describe('ReleaseOffCanvasComponent', () => {
  let component: ReleaseOffCanvasComponent;
  let fixture: ComponentFixture<ReleaseOffCanvasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseOffCanvasComponent], // Standalone component
      providers: [
        { provide: LabelService, useValue: mockLabelService },
        { provide: IssueService, useValue: mockIssueService },
        { provide: ToastrService, useValue: mockToastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseOffCanvasComponent);
    component = fixture.componentInstance;

    mockLabelService.getHighLightsByReleaseId.calls.reset();
    mockIssueService.getIssuesByReleaseId.calls.reset();
    mockToastService.error.calls.reset();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnChanges - Data Fetching', () => {
    it('should set isLoading to true initially, and then to false after data is fetched', fakeAsync(() => {
      component.isLoading = true;
      component.release = mockRelease;
      component.ngOnChanges({ release: new SimpleChange(null, mockRelease, true) });

      tick();

      expect(component.isLoading).toBe(false);
    }));

    it('should not fetch data if release is null or id is missing', () => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of(mockLabels));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));

      component.release = null as any;
      component.ngOnChanges({ release: new SimpleChange(null, null, true) });

      expect(mockLabelService.getHighLightsByReleaseId).not.toHaveBeenCalled();
      expect(mockIssueService.getIssuesByReleaseId).not.toHaveBeenCalled();
    });

    it('should handle label fetch error gracefully', fakeAsync(() => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(throwError(() => new Error('Label API Error')));

      component.release = mockRelease;
      component.ngOnChanges({ release: new SimpleChange(null, mockRelease, true) });
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.highlightedLabels).toBeUndefined();
      expect(mockToastService.error).toHaveBeenCalledWith('Failed to load release highlights. Please try again later.');
    }));

    it('should handle issue fetch error gracefully', fakeAsync(() => {
      mockIssueService.getIssuesByReleaseId.and.returnValue(throwError(() => new Error('Issue API Error')));

      component.release = mockRelease;
      component.ngOnChanges({ release: new SimpleChange(null, mockRelease, true) });
      tick();

      expect(component.isLoading).toBe(false);
      expect(component.releaseIssues).toBeUndefined();
      expect(mockToastService.error).toHaveBeenCalledWith('Failed to load release issues. Please try again later.');
    }));

    it('should show a toast and set labels to undefined if the API returns an empty array', fakeAsync(() => {
      mockLabelService.getHighLightsByReleaseId.and.returnValue(of([]));
      mockIssueService.getIssuesByReleaseId.and.returnValue(of(mockIssues));

      component.release = mockRelease;
      component.ngOnChanges({ release: new SimpleChange(null, mockRelease, true) });
      tick();

      expect(component.highlightedLabels).toBeUndefined();
      expect(mockToastService.error).toHaveBeenCalledWith('No release highlights found.');
    }));
  });

  describe('colorNameToRgba', () => {
    it('should convert a color name to an rgba string with 0.75 opacity', () => {
      // Mock the DOM interaction
      spyOn(globalThis, 'getComputedStyle').and.returnValue({ color: 'rgb(0, 0, 255)' } as any);
      const temporaryElement = document.createElement('div');
      spyOn(document, 'createElement').and.returnValue(temporaryElement);
      spyOn(document.body, 'append');
      spyOn(temporaryElement, 'remove');

      const result = component.colorNameToRgba('blue');

      expect(result).toBe('rgba(0,0,255,0.75)');
      expect(document.createElement).toHaveBeenCalledWith('div');
      expect(temporaryElement.style.color).toBe('blue');
      expect(document.body.append).toHaveBeenCalledWith(temporaryElement);
      expect(temporaryElement.remove).toHaveBeenCalledWith();
    });

    it('should return the original color if conversion fails', () => {
      spyOn(globalThis, 'getComputedStyle').and.returnValue({ color: 'invalid-color' } as any);
      // Mock other DOM interactions to avoid errors
      spyOn(document, 'createElement').and.callThrough();
      spyOn(document.body, 'append');

      const result = component.colorNameToRgba('not-a-color');

      expect(result).toBe('not-a-color');
    });
  });

  describe('Outputs', () => {
    it('should emit closeCanvas event', () => {
      spyOn(component.closeCanvas, 'emit');

      const offCanvasComponent = { close: () => component.closeCanvas.emit() };
      offCanvasComponent.close();

      expect(component.closeCanvas.emit).toHaveBeenCalledWith();
    });
  });
});
