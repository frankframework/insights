import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { FeedbackComponent } from './feedback.component';
import { VersionService } from '../../services/version.service';

describe('FeedbackComponent', () => {
  let component: FeedbackComponent;
  let fixture: ComponentFixture<FeedbackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FeedbackComponent],
      providers: [
        { provide: Router, useValue: { url: '/test-page' } },
        { provide: VersionService, useValue: { getBuildInformation: () => of({ version: '1.0.0' }) } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(FeedbackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
