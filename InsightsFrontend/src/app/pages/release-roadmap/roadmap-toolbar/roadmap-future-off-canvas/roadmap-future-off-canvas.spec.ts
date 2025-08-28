import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RoadmapFutureOffCanvas } from './roadmap-future-off-canvas';

describe('RoadmapFutureOffCanvas', () => {
  let component: RoadmapFutureOffCanvas;
  let fixture: ComponentFixture<RoadmapFutureOffCanvas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoadmapFutureOffCanvas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RoadmapFutureOffCanvas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
