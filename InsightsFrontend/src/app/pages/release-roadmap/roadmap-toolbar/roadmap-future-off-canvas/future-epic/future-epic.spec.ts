import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FutureEpic } from './future-epic';

describe('FutureEpic', () => {
  let component: FutureEpic;
  let fixture: ComponentFixture<FutureEpic>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FutureEpic]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FutureEpic);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
