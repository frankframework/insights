import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { OffCanvasComponent } from './off-canvas.component';

describe('OffCanvasComponent', () => {
  let component: OffCanvasComponent;
  let fixture: ComponentFixture<OffCanvasComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OffCanvasComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(OffCanvasComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
