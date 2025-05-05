import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseGraphComponent } from './release-graph.component';

describe('ReleaseGraphComponent', () => {
  let component: ReleaseGraphComponent;
  let fixture: ComponentFixture<ReleaseGraphComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseGraphComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReleaseGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
