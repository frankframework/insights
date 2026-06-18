import { Component, inject, Input, OnInit } from '@angular/core';
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
  imports: [ModalComponent, AsyncPipe, DatePipe, LowerCasePipe, GestureComponent, PillButtonComponent],
})
export class ReleaseCatalogusComponent implements OnInit {
  private static readonly SESSION_KEY = 'releaseCatalogusShown';

  @Input() showExtendedSupport = false;

  public modalOpen = false;
  public buildInfo$: Observable<BuildInfo | null> = inject(VersionService).getBuildInformation();

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
