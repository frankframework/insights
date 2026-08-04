import { Component, inject, Input, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { AsyncPipe, DatePipe, LowerCasePipe } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { BuildInfo, VersionService } from '../../../services/version.service';
import { Observable } from 'rxjs';
import { GestureComponent } from '../../../components/gesture/gesture.component';
import { PillButtonComponent } from '../../../components/pill-button/pill-button.component';

@Component({
  selector: 'app-release-catalogus',
  standalone: true,
  templateUrl: './release-catalogus.component.html',
  styleUrl: './release-catalogus.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [ModalComponent, AsyncPipe, DatePipe, LowerCasePipe, GestureComponent, PillButtonComponent],
})
export class ReleaseCatalogusComponent implements OnInit {
  private static readonly SESSION_KEY = 'releaseCatalogusShown';

  private static readonly MAJOR_PHASE_MONTHS = 6;
  private static readonly MINOR_PHASE_MONTHS = 3;

  @Input() extendedSupportLevel = 0;

  public modalOpen = false;
  public buildInfo$: Observable<BuildInfo | null> = inject(VersionService).getBuildInformation();

  public get showExtendedSupport(): boolean {
    return this.extendedSupportLevel > 0;
  }

  public get majorExtendedMonths(): number {
    return this.extendedSupportLevel * ReleaseCatalogusComponent.MAJOR_PHASE_MONTHS;
  }

  public get majorTotalMonths(): number {
    return ReleaseCatalogusComponent.MAJOR_PHASE_MONTHS * 2 + this.majorExtendedMonths;
  }

  public get minorExtendedMonths(): number {
    return this.extendedSupportLevel * ReleaseCatalogusComponent.MINOR_PHASE_MONTHS;
  }

  public get minorTotalMonths(): number {
    return ReleaseCatalogusComponent.MINOR_PHASE_MONTHS * 2 + this.minorExtendedMonths;
  }

  toggleModal(): void {
    this.modalOpen = !this.modalOpen;
  }

  ngOnInit(): void {
    if (!sessionStorage.getItem(ReleaseCatalogusComponent.SESSION_KEY)) {
      this.modalOpen = true;
      sessionStorage.setItem(ReleaseCatalogusComponent.SESSION_KEY, 'true');
    }
  }
}
