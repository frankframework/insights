import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MilestoneRowComponent } from './milestone-row.component';

describe('MilestoneRowComponent', () => {
  let component: MilestoneRowComponent;
  let fixture: ComponentFixture<MilestoneRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MilestoneRowComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(MilestoneRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
