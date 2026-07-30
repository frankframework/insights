import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-include-version-button',
  standalone: true,
  imports: [],
  templateUrl: './include-version-button.component.html',
  styleUrl: './include-version-button.component.scss',
})
export class IncludeVersionButtonComponent {
  @Input() version = '';
  @Input() included = false;
  @Input() pending = false;
  @Output() include = new EventEmitter<void>();
  @Output() remove = new EventEmitter<void>();

  public get title(): string {
    if (this.included) return `${this.version} is already shown in the graph`;
    if (this.pending) return `Click to remove ${this.version} from pending`;
    return `Show only ${this.version} in the graph`;
  }

  public onClick(event: Event): void {
    event.stopPropagation();
    if (this.pending) {
      this.remove.emit();
    } else {
      this.include.emit();
    }
  }
}
