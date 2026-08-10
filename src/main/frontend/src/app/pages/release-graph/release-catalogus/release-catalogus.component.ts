import { ChangeDetectionStrategy, Component, OnInit, Signal, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { BuildInfo, VersionService } from '../../../services/version.service';
import { GestureComponent } from '../../../components/gesture/gesture.component';
import { PillButtonComponent } from '../../../components/pill-button/pill-button.component';

@Component({
  selector: 'app-release-catalogus',
  standalone: true,
  templateUrl: './release-catalogus.component.html',
  styleUrl: './release-catalogus.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ModalComponent, DatePipe, LowerCasePipe, GestureComponent, PillButtonComponent],
})
export class ReleaseCatalogusComponent implements OnInit {
  private static readonly SESSION_KEY = 'releaseCatalogusShown';

  private static readonly MAJOR_PHASE_MONTHS = 6;
  private static readonly MINOR_PHASE_MONTHS = 3;

  public readonly extendedSupportLevel = input(0);

  public readonly modalOpen = signal(false);
  public readonly buildInfo: Signal<BuildInfo | null>;

  public readonly showExtendedSupport: Signal<boolean> = computed(() => this.extendedSupportLevel() > 0);
  public readonly majorExtendedMonths: Signal<number> = computed(
    () => this.extendedSupportLevel() * ReleaseCatalogusComponent.MAJOR_PHASE_MONTHS,
  );
  public readonly majorTotalMonths: Signal<number> = computed(
    () => ReleaseCatalogusComponent.MAJOR_PHASE_MONTHS * 2 + this.majorExtendedMonths(),
  );
  public readonly minorExtendedMonths: Signal<number> = computed(
    () => this.extendedSupportLevel() * ReleaseCatalogusComponent.MINOR_PHASE_MONTHS,
  );
  public readonly minorTotalMonths: Signal<number> = computed(
    () => ReleaseCatalogusComponent.MINOR_PHASE_MONTHS * 2 + this.minorExtendedMonths(),
  );

  constructor() {
    this.buildInfo = toSignal(inject(VersionService).getBuildInformation(), { initialValue: null });
  }

  toggleModal(): void {
    this.modalOpen.update((isOpen) => !isOpen);
  }

  ngOnInit(): void {
    if (!sessionStorage.getItem(ReleaseCatalogusComponent.SESSION_KEY)) {
      this.modalOpen.set(true);
      sessionStorage.setItem(ReleaseCatalogusComponent.SESSION_KEY, 'true');
    }
  }
}
