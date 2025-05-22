import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseGraphComponent } from './release-graph.component';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('ReleaseCoordinatePlaneComponent', () => {
  let component: ReleaseGraphComponent;
  let fixture: ComponentFixture<ReleaseGraphComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseGraphComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
