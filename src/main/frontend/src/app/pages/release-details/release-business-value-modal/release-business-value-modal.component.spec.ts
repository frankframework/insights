import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseBusinessValueModalComponent } from './release-business-value-modal.component';
import { Component, ChangeDetectionStrategy, input, output } from '@angular/core';
import { ModalComponent } from '../../../components/modal/modal.component';
import { IssueTreeBranchComponent } from '../release-important-issues/issue-tree-branch/issue-tree-branch.component';
import { Issue } from '../../../services/issue.service';

@Component({
  selector: 'app-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<ng-content></ng-content>',
})
class MockModalComponent {
  readonly title = input<string>('');
  readonly closed = output<void>();
}

@Component({
  selector: 'app-issue-tree-branch',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '',
})
class MockIssueTreeBranchComponent {
  readonly issue = input.required<Issue>();
  readonly depth = input<number>(0);
}

describe('ReleaseBusinessValueModalComponent', () => {
  let component: ReleaseBusinessValueModalComponent;
  let fixture: ComponentFixture<ReleaseBusinessValueModalComponent>;

  const mockIssue: Issue = {
    id: 'issue-1',
    number: 1,
    title: 'Test Issue',
    state: 'OPEN',
    url: 'http://github.com/test/1',
    labels: [],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseBusinessValueModalComponent],
    })
      .overrideComponent(ReleaseBusinessValueModalComponent, {
        remove: { imports: [ModalComponent, IssueTreeBranchComponent] },
        add: { imports: [MockModalComponent, MockIssueTreeBranchComponent] },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ReleaseBusinessValueModalComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('User Interactions', () => {
    it('should emit closed event when close() is called', () => {
      spyOn(component.closed, 'emit');
      component.close();

      expect(component.closed.emit).toHaveBeenCalledWith();
    });

    it('should open issue url in new tab', () => {
      spyOn(globalThis, 'open');
      component.openIssue(mockIssue);

      expect(globalThis.open).toHaveBeenCalledWith(mockIssue.url, '_blank');
    });

    it('should not open window if issue has no url', () => {
      spyOn(globalThis, 'open');
      const noUrlIssue = { ...mockIssue, url: undefined };
      component.openIssue(noUrlIssue as any);

      expect(globalThis.open).not.toHaveBeenCalled();
    });
  });
});
