import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IssueTreeBranchComponent } from './issue-tree-branch.component';

describe('IssueTreeBranchComponent', () => {
  let component: IssueTreeBranchComponent;
  let fixture: ComponentFixture<IssueTreeBranchComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueTreeBranchComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IssueTreeBranchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
