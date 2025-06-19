import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseRoadmapComponent } from './release-roadmap.component';

describe('ReleaseRoadmapComponent', () => {
  let component: ReleaseRoadmapComponent;
  let fixture: ComponentFixture<ReleaseRoadmapComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseRoadmapComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseRoadmapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
