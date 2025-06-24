import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoadmapToolbarComponent } from './roadmap-toolbar.component';

describe('RoadmapToolbarComponent', () => {
  let component: RoadmapToolbarComponent;
  let fixture: ComponentFixture<RoadmapToolbarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoadmapToolbarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RoadmapToolbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
