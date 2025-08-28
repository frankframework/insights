import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BehaviorSubject } from 'rxjs';
import { RoadmapFutureOffCanvasComponent } from './roadmap-future-off-canvas/roadmap-future-off-canvas';

@Component({
  selector: 'app-roadmap-toolbar',
  standalone: true,
  imports: [CommonModule, RoadmapFutureOffCanvasComponent, RoadmapFutureOffCanvasComponent],
  templateUrl: './roadmap-toolbar.component.html',
  styleUrls: ['./roadmap-toolbar.component.scss'],
})
export class RoadmapToolbarComponent {
  @Input() public periodLabel = '';
  @Output() public readonly changePeriod = new EventEmitter<number>();
  @Output() public readonly resetPeriod = new EventEmitter<void>();

  private readonly _isCanvasVisible = new BehaviorSubject<boolean>(false);
  // eslint-disable-next-line @typescript-eslint/member-ordering
  public readonly isCanvasVisible$ = this._isCanvasVisible.asObservable();

  public openFutureDetails(): void {
    this._isCanvasVisible.next(true);
  }

  public closeFutureDetails(): void {
    this._isCanvasVisible.next(false);
  }
}
