import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseImportantIssuesComponent } from './release-important-issues.component';

describe('ReleaseImportantIssuesComponent', () => {
  let component: ReleaseImportantIssuesComponent;
  let fixture: ComponentFixture<ReleaseImportantIssuesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseImportantIssuesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReleaseImportantIssuesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
