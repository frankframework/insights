import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReleaseCatalogusComponent } from './release-catalogus.component';

describe('ReleaseCatalogusComponent', () => {
  let component: ReleaseCatalogusComponent;
  let fixture: ComponentFixture<ReleaseCatalogusComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseCatalogusComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseCatalogusComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
