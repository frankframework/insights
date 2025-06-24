import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IssueBarComponent } from './issue-bar.component';

describe('IssueBarComponent', () => {
  let component: IssueBarComponent;
  let fixture: ComponentFixture<IssueBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IssueBarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(IssueBarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
