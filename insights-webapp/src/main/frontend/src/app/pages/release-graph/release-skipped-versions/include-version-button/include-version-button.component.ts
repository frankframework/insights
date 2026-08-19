import { Component, Signal, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-include-version-button',
  standalone: true,
  imports: [],
  templateUrl: './include-version-button.component.html',
  host: { class: 'contents' },
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

  public readonly buttonClasses: Signal<string> = computed(() => {
    const base =
      'ml-auto cursor-pointer rounded-xl border px-2 py-0.5 text-[0.7rem] font-semibold whitespace-nowrap ' +
      'transition-all duration-200 ease-in-out disabled:cursor-not-allowed disabled:border-gray-200 disabled:text-gray-400';

    if (this.pending()) {
      return `${base} border-teal-700 bg-white text-teal-700 hover:border-red-600 hover:bg-red-50 hover:text-red-600`;
    }

    return `${base} border-gray-300 bg-white text-gray-600 enabled:hover:border-blue-900 enabled:hover:text-blue-900`;
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
