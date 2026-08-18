import { Component } from '@angular/core';

@Component({
  selector: 'app-lts-badge',
  standalone: true,
  imports: [],
  template: ` <span
    class="lts-badge inline-block shrink-0 rounded bg-[linear-gradient(135deg,#9370DB_0%,#7B68BD_100%)] px-1.5 py-0.5 text-[10px] font-bold tracking-wider text-white uppercase shadow-[0_1px_3px_rgba(147,112,219,0.3)]"
  >
    LTS
  </span>`,
  host: { class: 'contents' },
})
export class LtsBadgeComponent {}
