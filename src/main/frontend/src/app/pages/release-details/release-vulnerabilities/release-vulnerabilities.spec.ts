import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseVulnerabilities } from './release-vulnerabilities';

describe('ReleaseVulnerabilities', () => {
  let component: ReleaseVulnerabilities;
  let fixture: ComponentFixture<ReleaseVulnerabilities>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseVulnerabilities]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReleaseVulnerabilities);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
