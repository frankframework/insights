import { Component, OnDestroy, OnInit } from '@angular/core';
import { NgStyle } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';

@Component({
  selector: 'app-release-catalogus',
  standalone: true,
  templateUrl: './release-catalogus.component.html',
  styleUrl: './release-catalogus.component.scss',
  imports: [NgStyle, ModalComponent],
})
export class ReleaseCatalogusComponent implements OnInit, OnDestroy {
  public modalOpen = false;
  public isSmallScreen = false;
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
  }

  ngOnDestroy(): void {
    if (this.mediaQueryList && this.mediaListener) {
      this.mediaQueryList.removeEventListener('change', this.mediaListener);
    }
  }
}
