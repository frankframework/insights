import { Component, Signal, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-include-version-button',
  standalone: true,
  imports: [],
  templateUrl: './include-version-button.component.html',
  styleUrl: './include-version-button.component.scss',
})
export class IncludeVersionButtonComponent {
  public readonly version = input<string>('');
  public readonly included = input<boolean>(false);
  public readonly pending = input<boolean>(false);

  public readonly include = output<void>();
  public readonly remove = output<void>();

  public readonly title: Signal<string> = computed(() => {
    const version = this.version();
    if (this.included()) return `${version} is already shown in the graph`;
    if (this.pending()) return `Click to remove ${version} from pending`;
    return `Include ${version} in the graph`;
  });

  public onClick(event: Event): void {
    event.stopPropagation();
    if (this.pending()) {
      this.remove.emit();
    } else {
      this.include.emit();
    }
  }
}
