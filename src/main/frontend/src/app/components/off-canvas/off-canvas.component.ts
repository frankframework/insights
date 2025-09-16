import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-off-canvas',
  imports: [],
  templateUrl: './off-canvas.component.html',
  styleUrl: './off-canvas.component.scss',
  standalone: true,
})
export class OffCanvasComponent {
  @Input() title?: string;
  @Output() closeCanvas = new EventEmitter<void>();

  public onClose(): void {
    this.closeCanvas.emit();
  }
}
