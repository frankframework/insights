import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-gesture',
  standalone: true,
  imports: [],
  templateUrl: './gesture.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './gesture.component.scss',
})
export class GestureComponent {
  @Input() imageSrc = '';
  @Input() gestureDescription = '';
}
