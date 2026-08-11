import { Component } from '@angular/core';
import { ISSUE_STATE_STYLES, IssueStateStyle } from '../roadmap.types';

interface LegendItem {
  label: string;
  style: IssueStateStyle;
}

const ISSUE_STATE_LEGEND_ITEMS: LegendItem[] = Object.entries(ISSUE_STATE_STYLES).map(([label, style]) => ({
  label,
  style,
}));

@Component({
  selector: 'app-roadmap-legend',
  imports: [],
  templateUrl: './roadmap-legend.component.html',
  styleUrl: './roadmap-legend.component.scss',
})
export class RoadmapLegend {
  public readonly issueStateItems: LegendItem[] = ISSUE_STATE_LEGEND_ITEMS;
}
