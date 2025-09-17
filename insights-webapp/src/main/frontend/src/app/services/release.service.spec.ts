import { TestBed } from '@angular/core/testing';

import { ReleaseService } from './release.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('ReleaseService', () => {
  let service: ReleaseService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReleaseService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
