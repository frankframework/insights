import { Component, ElementRef, afterNextRender, input, output, viewChild } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [],
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent {
  public readonly title = input('');
  public readonly hideScrollBar = input(false);
  public readonly closeOnBackdropClick = input(true);
  public readonly closed = output<void>();
  public readonly modalContent = viewChild.required<ElementRef<HTMLDivElement>>('modalContent');

  constructor() {
    afterNextRender(() => this.modalContent().nativeElement.focus());
  }

  public close(): void {
    this.closed.emit();
  }
}
