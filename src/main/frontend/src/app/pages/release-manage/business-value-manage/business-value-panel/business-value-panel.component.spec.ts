import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BusinessValuePanelComponent } from './business-value-panel.component';
import { BusinessValue } from '../../../../services/business-value.service';

const mockBusinessValues: BusinessValue[] = [
  { id: 'bv-1', title: 'Security Improvements', description: 'desc', issues: [{ id: 'i1' } as any] },
  { id: 'bv-2', title: 'UX Enhancements', description: 'desc', issues: [] },
  { id: 'bv-3', title: 'Performance', description: 'desc', issues: [{ id: 'i2' } as any, { id: 'i3' } as any] },
];

function createInputEvent(value: string): Event {
  return { target: { value } } as any;
}

describe('BusinessValuePanelComponent', () => {
  let component: BusinessValuePanelComponent;
  let fixture: ComponentFixture<BusinessValuePanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BusinessValuePanelComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BusinessValuePanelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('businessValues', mockBusinessValues);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Filtering and Sorting', () => {
    it('should filter business values by search query', () => {
      component.updateSearchQuery(createInputEvent('Security'));
      const filtered = component.filteredBusinessValues();

      expect(filtered.length).toBe(1);
      expect(filtered[0].title).toBe('Security Improvements');
    });

    it('should return all business values when search query is empty', () => {
      component.updateSearchQuery(createInputEvent(''));
      const filtered = component.filteredBusinessValues();

      expect(filtered.length).toBe(3);
    });

    it('should sort business values by issue count descending', () => {
      const sorted = component.filteredBusinessValues();

      expect(sorted[0].id).toBe('bv-3');
      expect(sorted[1].id).toBe('bv-1');
      expect(sorted[2].id).toBe('bv-2');
    });

    it('should be case insensitive when filtering', () => {
      component.updateSearchQuery(createInputEvent('performance'));
      const filtered = component.filteredBusinessValues();

      expect(filtered.length).toBe(1);
      expect(filtered[0].title).toBe('Performance');
    });
  });

  describe('Event Emissions', () => {
    it('should emit businessValueSelected when selecting a business value', () => {
      spyOn(component.businessValueSelected, 'emit');

      component.selectBusinessValue(mockBusinessValues[0]);

      expect(component.businessValueSelected.emit).toHaveBeenCalledWith(mockBusinessValues[0]);
    });

    it('should emit createClicked when add button is clicked', () => {
      spyOn(component.createClicked, 'emit');

      component.onCreateClick();

      expect(component.createClicked.emit).toHaveBeenCalledWith();
    });

    it('should emit editClicked when edit button is clicked', () => {
      spyOn(component.editClicked, 'emit');
      const event = new MouseEvent('click');

      component.onEditClick(event);

      expect(component.editClicked.emit).toHaveBeenCalledWith();
    });

    it('should emit deleteClicked with business value and event', () => {
      spyOn(component.deleteClicked, 'emit');
      const event = new MouseEvent('click');

      component.onDeleteClick(mockBusinessValues[0], event);

      expect(component.deleteClicked.emit).toHaveBeenCalledWith({
        businessValue: mockBusinessValues[0],
        event,
      });
    });
  });

  describe('Search Query Management', () => {
    it('should update search query signal when input changes', () => {
      const query = 'test query';
      component.updateSearchQuery(createInputEvent(query));

      expect(component.businessValueSearchQuery()).toBe(query);
    });

    it('should trim and lowercase search query for filtering', () => {
      component.updateSearchQuery(createInputEvent('  SECURITY  '));
      const filtered = component.filteredBusinessValues();

      expect(filtered.length).toBe(1);
      expect(filtered[0].title).toBe('Security Improvements');
    });
  });
});
