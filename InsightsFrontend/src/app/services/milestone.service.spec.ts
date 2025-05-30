import { TestBed } from '@angular/core/testing';

import { MilestoneService } from './milestone.service';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

describe('MilestoneService', () => {
  let service: MilestoneService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MilestoneService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
