import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseSkippedVersions } from './release-skipped-versions';

describe('ReleaseSkippedVersions', () => {
  let component: ReleaseSkippedVersions;
  let fixture: ComponentFixture<ReleaseSkippedVersions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseSkippedVersions]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReleaseSkippedVersions);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
