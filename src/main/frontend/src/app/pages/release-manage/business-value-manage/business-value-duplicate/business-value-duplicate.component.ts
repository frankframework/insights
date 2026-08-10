import { ChangeDetectionStrategy, Component, Signal, computed, input, output, signal } from '@angular/core';
import { ModalComponent } from '../../../../components/modal/modal.component';
import { Release } from '../../../../services/release.service';

@Component({
  selector: 'app-business-value-duplicate',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './business-value-duplicate.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './business-value-duplicate.component.scss',
})
export class BusinessValueDuplicateComponent {
  public readonly targetReleaseTitle = input.required<string>();
  public readonly releases = input.required<Release[]>();
  public readonly isDuplicating = input(false);
  public readonly errorMessage = input('');

  public readonly closed = output<void>();
  public readonly duplicateSelected = output<Release>();

  public readonly searchQuery = signal<string>('');

  public readonly filteredReleases: Signal<Release[]> = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.releases();
    return this.releases().filter((release) => release.name.toLowerCase().includes(query));
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
