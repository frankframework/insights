import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LocationService {
  navigateTo(url: string): void {
    globalThis.location.href = url;
  }

  reload(): void {
    globalThis.location.reload();
  }
}
