import { Component, EventEmitter, Input, Output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { Release } from '../../../../services/release.service';

@Component({
  selector: 'app-business-value-duplicate',
  standalone: true,
  imports: [CommonModule, ModalComponent],
  templateUrl: './business-value-duplicate.component.html',
  styleUrl: './business-value-duplicate.component.scss',
})
export class BusinessValueDuplicateComponent {
  @Input({ required: true }) targetReleaseTitle!: string;
  @Input({ required: true }) releases!: Release[];
  @Input() isDuplicating = false;
  @Input() errorMessage = '';

  @Output() closed = new EventEmitter<void>();
  @Output() duplicateSelected = new EventEmitter<Release>();

  public searchQuery = signal<string>('');

  public filteredReleases = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.releases;
    return this.releases.filter((r) => r.name.toLowerCase().includes(query));
  });

  public close(): void {
    this.searchQuery.set('');
    this.closed.emit();
  }

  public selectRelease(release: Release): void {
    this.duplicateSelected.emit(release);
  }

  public updateSearchQuery(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }
}
