import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-gesture',
  standalone: true,
  imports: [],
  templateUrl: './gesture.component.html',
  styleUrl: './gesture.component.scss',
})
export class GestureComponent {
  @Input() imageSrc = '';
  @Input() gestureDescription = '';
}
