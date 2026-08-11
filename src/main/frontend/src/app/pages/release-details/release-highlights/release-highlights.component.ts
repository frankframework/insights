import { Component, Signal, computed, inject, input } from '@angular/core';
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Label } from '../../../services/label.service';
import { Issue } from '../../../services/issue.service';
import { ColorService } from '../../../services/color.service';

Chart.register(DoughnutController, ArcElement, Tooltip, Legend);

interface LegendItem {
  label: string;
  color: string;
  count: number;
}

interface ChartModel {
  data: ChartConfiguration<'doughnut'>['data'];
  legendItems: LegendItem[];
  highlightLegendItems: LegendItem[];
  hasHighlightRing: boolean;
}

const MAX_LABELS = 20;
const EMPTY_CHART_MODEL: ChartModel = {
  data: { labels: [], datasets: [] },
  legendItems: [],
  highlightLegendItems: [],
  hasHighlightRing: false,
};

const normalizeColor = (color: string): string => color.replace('#', '').toLowerCase();

@Component({
  selector: 'app-release-highlights',
  standalone: true,
  imports: [BaseChartDirective],
  templateUrl: './release-highlights.component.html',
  styleUrl: './release-highlights.component.scss',
})
export class ReleaseHighlightsComponent {
  public readonly highlightedLabels = input<Label[] | null>(null);
  public readonly releaseIssues = input<Issue[] | null>(null);

  public readonly doughnutChartOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '50%',
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        enabled: true,
      },
    },
  };

  public readonly doughnutChartPlugins = [];

  public readonly sortedHighlightedLabels: Signal<Label[]> = computed(() => {
    const highlightedLabels = this.highlightedLabels();
    if (!highlightedLabels) return [];

    return [...highlightedLabels].toSorted((labelA, labelB) => {
      const colorComparison = normalizeColor(labelA.color || '').localeCompare(normalizeColor(labelB.color || ''));
      if (colorComparison !== 0) return colorComparison;
      return (labelA.name || '').localeCompare(labelB.name || '');
    });
  });

  public readonly doughnutChartData: Signal<ChartConfiguration<'doughnut'>['data']> = computed(
    () => this.chartModel().data,
  );
  public readonly legendItems: Signal<LegendItem[]> = computed(() => this.chartModel().legendItems);
  public readonly highlightLegendItems: Signal<LegendItem[]> = computed(() => this.chartModel().highlightLegendItems);
  public readonly hasHighlightRing: Signal<boolean> = computed(() => this.chartModel().hasHighlightRing);

  private readonly colorService = inject(ColorService);

  private readonly chartModel: Signal<ChartModel> = computed(() => {
    const releaseIssues = this.releaseIssues();
    if (!releaseIssues) return EMPTY_CHART_MODEL;

    return this.composeChartModel(this.buildIssueTypeEntries(releaseIssues), this.buildHighlightEntries(releaseIssues));
  });

  public getDotColor(color: string): string {
    return color?.startsWith('#') ? color : `#${color}`;
  }

  private buildIssueTypeEntries(releaseIssues: Issue[]): LegendItem[] {
    const pieDataMap = new Map<string, { count: number; color: string; originalColor: string }>();

    for (const issue of releaseIssues) {
      if (!issue.issueType) continue;
      const issueTypeName = issue.issueType.name;
      const originalColor = issue.issueType.color;

      if (pieDataMap.has(issueTypeName)) {
        pieDataMap.get(issueTypeName)!.count += 1;
      } else {
        pieDataMap.set(issueTypeName, {
          count: 1,
          color: this.colorService.colorNameToRgba(originalColor),
          originalColor,
        });
      }
    }

    return [...pieDataMap.entries()]
      .toSorted(([nameA, dataA], [nameB, dataB]) => {
        const colorComparison = normalizeColor(dataA.originalColor).localeCompare(normalizeColor(dataB.originalColor));
        if (colorComparison !== 0) return colorComparison;
        return nameA.localeCompare(nameB);
      })
      .map(([label, { count, color }]) => ({ label, color, count }));
  }

  private buildHighlightEntries(releaseIssues: Issue[]): LegendItem[] {
    return this.sortedHighlightedLabels()
      .map((label) => ({
        label: label.name,
        color: this.colorService.colorNameToRgba(this.getDotColor(label.color)),
        originalColor: normalizeColor(label.color || ''),
        count: releaseIssues.filter((issue) => issue.labels?.some((issueLabel) => issueLabel.id === label.id)).length,
      }))
      .filter((entry) => entry.count > 0)
      .toSorted(
        (entryA, entryB) => entryA.originalColor.localeCompare(entryB.originalColor) || entryB.count - entryA.count,
      )
      .slice(0, MAX_LABELS)
      .map(({ label, color, count }) => ({ label, color, count }));
  }

  private composeChartModel(issueTypeEntries: LegendItem[], highlightEntries: LegendItem[]): ChartModel {
    const hasHighlightRing = highlightEntries.length > 0;
    const innerLabels = issueTypeEntries.map((entry) => entry.label);
    const innerData = issueTypeEntries.map((entry) => entry.count);
    const innerColors = issueTypeEntries.map((entry) => entry.color);

    if (!hasHighlightRing) {
      return {
        data: {
          labels: innerLabels,
          datasets: [{ data: innerData, backgroundColor: innerColors, borderWidth: 2, borderColor: '#ffffff' }],
        },
        legendItems: issueTypeEntries,
        highlightLegendItems: [],
        hasHighlightRing,
      };
    }

    const outerLabels = highlightEntries.map((entry) => entry.label);
    const outerData = highlightEntries.map((entry) => entry.count);
    const outerColors = highlightEntries.map((entry) => entry.color);

    return {
      data: {
        labels: [...outerLabels, ...innerLabels],
        datasets: [
          {
            data: [...outerData, ...innerLabels.map(() => 0)],
            backgroundColor: [...outerColors, ...innerLabels.map(() => 'transparent')],
            borderWidth: 2,
            borderColor: '#ffffff',
          },
          {
            data: [...outerLabels.map(() => 0), ...innerData],
            backgroundColor: [...outerLabels.map(() => 'transparent'), ...innerColors],
            borderWidth: 2,
            borderColor: '#ffffff',
          },
        ],
      },
      legendItems: issueTypeEntries,
      highlightLegendItems: highlightEntries,
      hasHighlightRing,
    };
  }
}
