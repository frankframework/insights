import { TestBed } from '@angular/core/testing';
import { ColorService } from './color.service';

describe('ColorService', () => {
  let service: ColorService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ColorService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getTypeTextColor', () => {
    it('should return "white" for a dark color like navy', () => {
      expect(service.getTypeTextColor('navy')).toBe('white');
    });

    it('should return "black" for a light color like yellow', () => {
      expect(service.getTypeTextColor('yellow')).toBe('black');
    });

    it('should return "black" for a light hex color like #fafad2 (lightgoldenrodyellow)', () => {
      expect(service.getTypeTextColor('#fafad2')).toBe('black');
    });

    it('should return "white" for a dark hex color like #8B0000 (darkred)', () => {
      expect(service.getTypeTextColor('#8B0000')).toBe('white');
    });

    it('should return "white" for an empty or invalid color string', () => {
      expect(service.getTypeTextColor('')).toBe('white');
      expect(service.getTypeTextColor('invalid-color-name')).toBe('white');
    });
  });

  describe('colorNameToRgba', () => {
    it('should convert a named color to an rgba string', () => {
      const expectedRgba = 'rgba(255,0,0,0.75)';

      expect(service.colorNameToRgba('red')).toBe(expectedRgba);
    });

    it('should handle hex codes and convert them to rgba', () => {
      const expectedRgba = 'rgba(0,0,255,0.75)';

      expect(service.colorNameToRgba('#0000ff')).toBe(expectedRgba);
    });

    it('should return a default rgba for an invalid color string', () => {
      const expectedRgba = 'rgba(0,0,0,0.75)';

      expect(service.colorNameToRgba('not-a-real-color')).toBe(expectedRgba);
    });
  });
});
