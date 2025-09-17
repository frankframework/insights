import { TestBed } from '@angular/core/testing';

import { LabelService } from './label.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('LabelService', () => {
  let service: LabelService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LabelService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
