import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  ChangeDetectionStrategy,
} from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [],
  templateUrl: './modal.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './modal.component.scss',
})
export class ModalComponent implements AfterViewInit {
  @Input() title = '';
  @Input() hideScrollBar = false;
  @Input() closeOnBackdropClick = true;
  @Output() closed = new EventEmitter<void>();
  @ViewChild('modalContent') modalContent!: ElementRef<HTMLDivElement>;

  public close(): void {
    this.closed.emit();
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.modalContent.nativeElement.focus());
  }
}
