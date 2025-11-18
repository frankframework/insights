import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ReleaseCatalogusComponent } from './release-catalogus.component';

describe('ReleaseCatalogusComponent', () => {
  let component: ReleaseCatalogusComponent;
  let fixture: ComponentFixture<ReleaseCatalogusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseCatalogusComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseCatalogusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
