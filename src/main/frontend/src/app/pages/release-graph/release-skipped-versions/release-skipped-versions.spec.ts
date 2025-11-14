import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ReleaseSkippedVersions } from './release-skipped-versions';

describe('ReleaseSkippedVersions', () => {
  let component: ReleaseSkippedVersions;
  let fixture: ComponentFixture<ReleaseSkippedVersions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseSkippedVersions],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
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
