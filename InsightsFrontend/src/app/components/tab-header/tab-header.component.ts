import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-tab-header',
  imports: [NgOptimizedImage],
  templateUrl: './tab-header.component.html',
  styleUrl: './tab-header.component.scss',
})
export class TabHeaderComponent {
  constructor(private router: Router) {}
  go(path: string): void {
    this.router.navigate([path]);
  }
  isActive(path: string): boolean {
    return this.router.url === path;
  }
}
