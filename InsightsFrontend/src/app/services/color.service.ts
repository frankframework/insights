import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class ColorService {
  public getTypeTextColor(color: string): string {
    const rgba = this.colorNameToRgba(color.trim().toLowerCase());

    const match = rgba.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
    if (!match) return 'white';

    const r = Number.parseInt(match[1], 10);
    const g = Number.parseInt(match[2], 10);
    const b = Number.parseInt(match[3], 10);

    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.7 ? 'black' : 'white';
  }

  public colorNameToRgba(color: string): string {
    const temporaryElement = document.createElement('div');
    temporaryElement.style.color = color;
    document.body.append(temporaryElement);

    const rgb = getComputedStyle(temporaryElement).color;
    temporaryElement.remove();

    const match = rgb.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
    if (match) {
      const [, r, g, b] = match;
      return `rgba(${r},${g},${b},${0.75})`;
    }

    return color;
  }
}
