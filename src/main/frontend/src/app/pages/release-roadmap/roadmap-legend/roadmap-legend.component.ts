import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ISSUE_STATE_STYLES, CLOSED_STYLE, OPEN_STYLE, IssueStateStyle } from '../release-roadmap.component';

interface LegendItem {
  label: string;
  style: IssueStateStyle;
}

@Component({
  selector: 'app-roadmap-legend',
  imports: [CommonModule],
  templateUrl: './roadmap-legend.component.html',
  styleUrl: './roadmap-legend.component.scss',
})
export class RoadmapLegend {
  public issueStateItems: LegendItem[] = [];
  public githubStateItems: LegendItem[] = [];

  constructor() {
    this.initializeLegendItems();
  }

  private initializeLegendItems(): void {
    for (const [stateName, style] of Object.entries(ISSUE_STATE_STYLES)) {
      this.issueStateItems.push({
        label: stateName,
        style: style,
      });
    }

    this.githubStateItems.push(
      {
        label: 'Merged',
        style: CLOSED_STYLE,
      },
      {
        label: 'Open',
        style: OPEN_STYLE,
      },
    );
  }
}
