import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseCoordinatePlaneComponent } from './release-graph.component';

describe('ReleaseCoordinatePlaneComponent', () => {
  let component: ReleaseCoordinatePlaneComponent;
  let fixture: ComponentFixture<ReleaseCoordinatePlaneComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseCoordinatePlaneComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReleaseCoordinatePlaneComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
