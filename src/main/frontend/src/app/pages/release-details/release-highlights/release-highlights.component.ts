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
    cutout: '70%',
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
  public sortedHighlightedLabels: Label[] = [];

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

    const sortedEntries = [...pieDataMap.entries()].toSorted(([nameA, dataA], [nameB, dataB]) => {
      const normalizedColorA = this.normalizeColor(dataA.originalColor);
      const normalizedColorB = this.normalizeColor(dataB.originalColor);
      const colorComparison = normalizedColorA.localeCompare(normalizedColorB);
      if (colorComparison !== 0) return colorComparison;
      return nameA.localeCompare(nameB);
    });

    const labels = sortedEntries.map(([name]) => name);
    const data = sortedEntries.map(([, { count }]) => count);
    const backgroundColor = sortedEntries.map(([, { color }]) => color);

    this.doughnutChartData = {
      labels,
      datasets: [
        {
          data,
          backgroundColor,
        },
      ],
    };

    this.legendItems = sortedEntries.map(([label, { count, color }]) => ({
      label,
      color,
      count,
    }));
  }

  private normalizeColor(color: string): string {
    return color.replace('#', '').toLowerCase();
  }
}
