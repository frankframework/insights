import { Component, Input, OnChanges } from '@angular/core';
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js';
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Label } from '../../../../services/label.service';
import { Issue } from '../../../../services/issue.service';

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

  ngOnChanges(): void {
    this.generatePieData();
  }

  public getDotColor(color: string): string {
    return color?.startsWith('#') ? color : `#${color}`;
  }

  private generatePieData(): void {
    if (!this.releaseIssues) return;

    const pieDataMap = new Map<string, { count: number; color: string }>();

    for (const issue of this.releaseIssues) {
      if (!issue.issueType) continue;
      const issueTypeName = issue.issueType.name;
      let issueTypeColor = issue.issueType.color;
      issueTypeColor = this.colorNameToRgba(issueTypeColor, 0.8);

      if (pieDataMap.has(issueTypeName)) {
        pieDataMap.get(issueTypeName)!.count += 1;
      } else {
        pieDataMap.set(issueTypeName, { count: 1, color: issueTypeColor });
      }
    }

    const labels = [...pieDataMap.keys()];
    const data = [...pieDataMap.values()].map((v) => v.count);
    const backgroundColor = [...pieDataMap.values()].map((v) => v.color);

    this.doughnutChartData = {
      labels,
      datasets: [
        {
          data,
          backgroundColor,
        },
      ],
    };
  }

  private colorNameToRgba(color: string, alpha: number): string {
    const temporaryElement = document.createElement('div');
    temporaryElement.style.color = color;
    document.body.append(temporaryElement);

    const rgb = getComputedStyle(temporaryElement).color;
    temporaryElement.remove();

    const match = rgb.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
    if (match) {
      const [, r, g, b] = match;
      return `rgba(${r},${g},${b},${alpha})`;
    }

    return color;
  }
}
