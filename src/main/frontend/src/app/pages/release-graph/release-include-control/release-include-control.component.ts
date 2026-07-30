import { Component, EventEmitter, Input, Output } from '@angular/core';
import { IncludeRange, serializeIncludeRanges } from '../../../pipes/release-include';

@Component({
  selector: 'app-release-include-control',
  standalone: true,
  imports: [],
  templateUrl: './release-include-control.component.html',
  styleUrl: './release-include-control.component.scss',
})
export class ReleaseIncludeControlComponent {
  @Input() includedReleases: IncludeRange[] = [];
  @Output() cleared = new EventEmitter<void>();

  public get isActive(): boolean {
    return this.includedReleases.length > 0;
  }

  public get label(): string {
    return serializeIncludeRanges(this.includedReleases);
  }

  public clear(): void {
    this.cleared.emit();
  }
}
