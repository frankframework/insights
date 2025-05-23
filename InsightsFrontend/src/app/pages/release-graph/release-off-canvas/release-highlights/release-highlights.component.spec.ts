import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseHighlightsComponent } from './release-highlights.component';

describe('ReleaseHighlightsComponent', () => {
  let component: ReleaseHighlightsComponent;
  let fixture: ComponentFixture<ReleaseHighlightsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseHighlightsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseHighlightsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
