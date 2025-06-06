import { Component, Input } from '@angular/core';
import { ModalComponent } from '../modal/modal.component';
import { Release } from '../../services/release.service';
import { NgStyle } from '@angular/common';

@Component({
  selector: 'app-release-catalogus',
  imports: [ModalComponent, NgStyle],
  templateUrl: './release-catalogus.component.html',
  styleUrl: './release-catalogus.component.scss',
})
export class ReleaseCatalogusComponent {
  @Input() selectedRelease: Release | null = null;
  public modalOpen = false;

  public toggleModal(): void {
    this.modalOpen = !this.modalOpen;
  }
}
