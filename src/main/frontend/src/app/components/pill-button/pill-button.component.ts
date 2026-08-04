import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

export type PillButtonIcon = 'moon' | 'help' | 'github';

@Component({
  selector: 'app-pill-button',
  standalone: true,
  imports: [],
  templateUrl: './pill-button.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './pill-button.component.scss',
})
export class PillButtonComponent {
  @Input() icon: PillButtonIcon = 'help';
  @Input() label = '';
  @Input() active = false;
  @Input() disabled = false;
  @Input() loading = false;
  @Input() tooltip = '';

  @Output() clicked = new EventEmitter<void>();
}
