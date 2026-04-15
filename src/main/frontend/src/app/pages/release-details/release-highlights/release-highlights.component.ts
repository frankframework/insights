import { Component, Input, OnChanges, inject } from '@angular/core';
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Label } from '../../../services/label.service';
import { Issue } from '../../../services/issue.service';
import { ColorService } from '../../../services/color.service';

Chart.register(DoughnutController, ArcElement, Tooltip, Legend);

@Component({
  selector: 'app-release-highlights',
  standalone: true,
  imports: [BaseChartDirective],
  templateUrl: './release-highlights.component.html',
  styleUrl: './release-highlights.component.scss',
})
export class ReleaseHighlightsComponent implements OnChanges {
  @Input() highlightedLabels?: Label[] = [];
  @Input() releaseIssues?: Issue[] = [];

  public doughnutChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: [],
    datasets: [],
  };

  public doughnutChartOptions: ChartOptions<'doughnut'> = {
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

  public doughnutChartPlugins = [];
  public legendItems: { label: string; color: string; count: number }[] = [];
  public highlightLegendItems: { label: string; color: string; count: number }[] = [];
  public sortedHighlightedLabels: Label[] = [];
  public hasHighlightRing = false;

  private colorService = inject(ColorService);

  ngOnChanges(): void {
    this.sortHighlightedLabels();
    this.generatePieData();
  }

  public getDotColor(color: string): string {
    return color?.startsWith('#') ? color : `#${color}`;
  }

  private sortHighlightedLabels(): void {
    if (!this.highlightedLabels) {
      this.sortedHighlightedLabels = [];
      return;
    }

    this.sortedHighlightedLabels = [...this.highlightedLabels].toSorted((a, b) => {
      const normalizedColorA = this.normalizeColor(a.color || '');
      const normalizedColorB = this.normalizeColor(b.color || '');
      const colorComparison = normalizedColorA.localeCompare(normalizedColorB);
      if (colorComparison !== 0) return colorComparison;
      return (a.name || '').localeCompare(b.name || '');
    });
  }

  private generatePieData(): void {
    if (!this.releaseIssues) return;

    // --- Inner ring: issue types ---
    const pieDataMap = new Map<string, { count: number; color: string; originalColor: string }>();

    for (const issue of this.releaseIssues) {
      if (!issue.issueType) continue;
      const issueTypeName = issue.issueType.name;
      const originalColor = issue.issueType.color;
      const displayColor = this.colorService.colorNameToRgba(originalColor);

      if (pieDataMap.has(issueTypeName)) {
        pieDataMap.get(issueTypeName)!.count += 1;
      } else {
        pieDataMap.set(issueTypeName, { count: 1, color: displayColor, originalColor });
      }
    }

    const sortedInnerEntries = [...pieDataMap.entries()].toSorted(([nameA, dataA], [nameB, dataB]) => {
      const colorComparison = this.normalizeColor(dataA.originalColor).localeCompare(
        this.normalizeColor(dataB.originalColor),
      );
      if (colorComparison !== 0) return colorComparison;
      return nameA.localeCompare(nameB);
    });

    const innerLabels = sortedInnerEntries.map(([name]) => name);
    const innerData = sortedInnerEntries.map(([, { count }]) => count);
    const innerColors = sortedInnerEntries.map(([, { color }]) => color);

    // --- Outer ring: highlighted labels ---
    const outerLabels: string[] = [];
    const outerData: number[] = [];
    const outerColors: string[] = [];

    for (const label of this.sortedHighlightedLabels) {
      const count = this.releaseIssues.filter((issue) => issue.labels?.some((l) => l.id === label.id)).length;
      if (count > 0) {
        outerLabels.push(label.name);
        outerData.push(count);
        outerColors.push(this.colorService.colorNameToRgba(this.getDotColor(label.color)));
      }
    }

    this.hasHighlightRing = outerLabels.length > 0;

    // Pad both datasets to the same length so Chart.js aligns tooltips correctly.
    // Zero-value segments are invisible and non-hoverable.
    const allLabels = [...innerLabels, ...outerLabels];
    const paddedInnerData = [...innerData, ...outerLabels.map(() => 0)];
    const paddedInnerColors = [...innerColors, ...outerLabels.map(() => 'transparent')];
    const paddedOuterData = [...innerLabels.map(() => 0), ...outerData];
    const paddedOuterColors = [...innerLabels.map(() => 'transparent'), ...outerColors];

    this.doughnutChartData = {
      labels: allLabels,
      datasets: [
        {
          data: paddedInnerData,
          backgroundColor: paddedInnerColors,
          borderWidth: 2,
          borderColor: '#ffffff',
        },
        ...(this.hasHighlightRing
          ? [
              {
                data: paddedOuterData,
                backgroundColor: paddedOuterColors,
                borderWidth: 2,
                borderColor: '#ffffff',
              },
            ]
          : []),
      ],
    };

    this.legendItems = sortedInnerEntries.map(([label, { count, color }]) => ({
      label,
      color,
      count,
    }));

    this.highlightLegendItems = outerLabels.map((label, index) => ({
      label,
      color: outerColors[index],
      count: outerData[index],
    }));
  }

  private normalizeColor(color: string): string {
    return color.replace('#', '').toLowerCase();
  }
}
