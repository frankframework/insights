import { Component, ChangeDetectionStrategy } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-not-found',
  imports: [NgOptimizedImage],
  templateUrl: './not-found.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './not-found.component.scss',
})
export class NotFoundComponent {}
