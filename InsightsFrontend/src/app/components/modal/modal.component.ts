import { AfterViewInit, Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [],
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent implements AfterViewInit {
  @Input() title = '';
  @Output() closed = new EventEmitter<void>();
  @ViewChild('modalContent') modalContent!: ElementRef<HTMLDivElement>;

  public close(): void {
    this.closed.emit();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.modalContent.nativeElement.focus());
  }
}
