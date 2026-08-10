import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-off-canvas',
  imports: [],
  templateUrl: './off-canvas.component.html',
  styleUrl: './off-canvas.component.scss',
  standalone: true,
})
export class OffCanvasComponent {
  public readonly title = input<string>();
  public readonly closeCanvas = output<void>();

  public onClose(): void {
    this.closeCanvas.emit();
  }
}
