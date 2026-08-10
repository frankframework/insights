import { Component, ChangeDetectionStrategy, input } from '@angular/core';

@Component({
  selector: 'app-gesture',
  standalone: true,
  imports: [],
  templateUrl: './gesture.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './gesture.component.scss',
})
export class GestureComponent {
  readonly imageSrc = input('');
  readonly gestureDescription = input('');
}
