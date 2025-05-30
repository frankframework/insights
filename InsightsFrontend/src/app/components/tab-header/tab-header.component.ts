import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-tab-header',
  imports: [NgOptimizedImage, RouterLink, RouterLinkActive],
  templateUrl: './tab-header.component.html',
  styleUrl: './tab-header.component.scss',
})
export class TabHeaderComponent {}
