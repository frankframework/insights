import { Component, input } from '@angular/core';

@Component({
  selector: 'app-gesture',
  standalone: true,
  imports: [],
  templateUrl: './gesture.component.html',
})
export class GestureComponent {
  readonly imageSrc = input('');
  readonly gestureDescription = input('');
}
