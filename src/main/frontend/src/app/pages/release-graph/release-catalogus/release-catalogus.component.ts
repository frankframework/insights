import { Component, inject, Input, OnDestroy, OnInit } from '@angular/core';
import { AsyncPipe, DatePipe, LowerCasePipe, NgStyle } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { AuthService } from '../../../services/auth.service';
import { BuildInfo, VersionService } from '../../../services/version.service';
import { Observable } from 'rxjs';
import { GestureComponent } from '../../../components/gesture/gesture.component';

@Component({
  selector: 'app-release-catalogus',
  standalone: true,
  templateUrl: './release-catalogus.component.html',
  styleUrl: './release-catalogus.component.scss',
  imports: [NgStyle, ModalComponent, AsyncPipe, DatePipe, LowerCasePipe, GestureComponent],
})
export class ReleaseCatalogusComponent implements OnInit, OnDestroy {
  private static readonly SESSION_KEY = 'releaseCatalogusShown';

  @Input() showExtendedSupport = false;

  public modalOpen = false;
  public isSmallScreen = false;
  public buildInfo$: Observable<BuildInfo | null> = inject(VersionService).getBuildInformation();

  protected authService: AuthService = inject(AuthService);

  private mediaQueryList: MediaQueryList | null = null;
  private mediaListener: (() => void) | null = null;

  toggleModal(): void {
    this.modalOpen = !this.modalOpen;
  }

  ngOnInit(): void {
    this.mediaQueryList = globalThis.matchMedia('(max-width: 992px)');
    this.isSmallScreen = this.mediaQueryList?.matches ?? false;
    this.mediaListener = (): void => {
      this.isSmallScreen = this.mediaQueryList?.matches ?? false;
    };
    this.mediaQueryList?.addEventListener('change', this.mediaListener);

    if (!sessionStorage.getItem(ReleaseCatalogusComponent.SESSION_KEY)) {
      this.modalOpen = true;
      sessionStorage.setItem(ReleaseCatalogusComponent.SESSION_KEY, 'true');
    }
  }

  ngOnDestroy(): void {
    if (this.mediaQueryList && this.mediaListener) {
      this.mediaQueryList.removeEventListener('change', this.mediaListener);
    }
  }
}
