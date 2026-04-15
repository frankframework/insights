import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseBusinessValueComponent } from './release-business-value.component';
import { BusinessValue } from '../../../services/business-value.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

const mockBusinessValues: BusinessValue[] = [
  { id: '1', title: 'Value 1', description: 'Description 1' },
  { id: '2', title: 'Value 2', description: 'Description 2' },
];

describe('ReleaseBusinessValueComponent', () => {
  let component: ReleaseBusinessValueComponent;
  let fixture: ComponentFixture<ReleaseBusinessValueComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseBusinessValueComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseBusinessValueComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('selectedBusinessValue signal', () => {
    it('should initialize selectedBusinessValue as null', () => {
      expect(component.selectedBusinessValue()).toBeNull();
    });

    it('should set selectedBusinessValue when openBusinessValueModal is called', () => {
      component.openBusinessValueModal(mockBusinessValues[0]);

      expect(component.selectedBusinessValue()).toEqual(mockBusinessValues[0]);
    });

    it('should clear selectedBusinessValue when closeModal is called', () => {
      component.openBusinessValueModal(mockBusinessValues[0]);
      component.closeModal();

      expect(component.selectedBusinessValue()).toBeNull();
    });
  });

  describe('rendering with businessValues input', () => {
    it('should render a list item for each business value', () => {
      component.businessValues = mockBusinessValues;
      fixture.detectChanges();

      const items = fixture.nativeElement.querySelectorAll('.business-value-item');

      expect(items.length).toBe(2);
    });

    it('should display title and description of each business value', () => {
      component.businessValues = mockBusinessValues;
      fixture.detectChanges();

      const titles = fixture.nativeElement.querySelectorAll('.business-value-title');

      expect(titles[0].textContent.trim()).toBe('Value 1');
      expect(titles[1].textContent.trim()).toBe('Value 2');

      const descriptions = fixture.nativeElement.querySelectorAll('.business-value-description');

      expect(descriptions[0].textContent.trim()).toBe('Description 1');
      expect(descriptions[1].textContent.trim()).toBe('Description 2');
    });

    it('should show empty message when businessValues is undefined', () => {
      component.businessValues = undefined;
      fixture.detectChanges();

      const emptyMessage = fixture.nativeElement.querySelector('.no-business-values');

      expect(emptyMessage).toBeTruthy();
      expect(emptyMessage.textContent.trim()).toBe('No business values found for this release.');
    });

    it('should show empty message when businessValues is an empty array', () => {
      component.businessValues = [];
      fixture.detectChanges();

      const emptyMessage = fixture.nativeElement.querySelector('.no-business-values');

      expect(emptyMessage).toBeTruthy();
    });

    it('should not render list when businessValues is empty', () => {
      component.businessValues = [];
      fixture.detectChanges();

      const list = fixture.nativeElement.querySelector('.business-values-list');

      expect(list).toBeNull();
    });
  });

  describe('modal interaction', () => {
    it('should open modal when clicking a business value item', () => {
      component.businessValues = mockBusinessValues;
      fixture.detectChanges();

      const item = fixture.nativeElement.querySelector('.business-value-item');
      item.click();
      fixture.detectChanges();

      expect(component.selectedBusinessValue()).toEqual(mockBusinessValues[0]);
    });

    it('should show modal when selectedBusinessValue is set', () => {
      component.businessValues = mockBusinessValues;
      fixture.detectChanges();

      component.openBusinessValueModal(mockBusinessValues[0]);
      fixture.detectChanges();

      const modal = fixture.nativeElement.querySelector('app-release-business-value-modal');

      expect(modal).toBeTruthy();
    });

    it('should hide modal when selectedBusinessValue is null', () => {
      component.businessValues = mockBusinessValues;
      fixture.detectChanges();

      const modal = fixture.nativeElement.querySelector('app-release-business-value-modal');

      expect(modal).toBeNull();
    });
  });
});
