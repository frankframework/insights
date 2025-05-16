import { TestBed } from '@angular/core/testing';

import { MilestoneService } from './milestone.service';

describe('MilestoneService', () => {
  let service: MilestoneService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MilestoneService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
