import { Component, computed, input, output } from '@angular/core';
import { ViewMode } from '../roadmap.types';

const TOGGLE_BASE =
  'toggle-button relative flex cursor-pointer items-center justify-center gap-1 rounded-md bg-transparent ' +
  'px-3 py-2 text-sm font-medium transition-all duration-200';

const TOGGLE_ACTIVE = 'active bg-white text-gray-800 shadow-sm';
const TOGGLE_IDLE = 'text-gray-500 hover:bg-white/50 hover:opacity-100';

@Component({
  selector: 'app-roadmap-toolbar',
  standalone: true,
  imports: [],
  templateUrl: './roadmap-toolbar.component.html',
})
export class RoadmapToolbarComponent {
  public readonly periodLabel = input('');
  public readonly viewMode = input<ViewMode>(ViewMode.QUARTERLY);
  public readonly changePeriod = output<number>();
  public readonly resetPeriod = output<void>();
  public readonly toggleViewMode = output<void>();

  public ViewMode = ViewMode;

  public readonly quarterlyToggleClasses = computed(
    () => `${TOGGLE_BASE} ${this.viewMode() === ViewMode.QUARTERLY ? TOGGLE_ACTIVE : TOGGLE_IDLE}`,
  );

  public readonly monthlyToggleClasses = computed(
    () => `${TOGGLE_BASE} ${this.viewMode() === ViewMode.MONTHLY ? TOGGLE_ACTIVE : TOGGLE_IDLE}`,
  );
}
