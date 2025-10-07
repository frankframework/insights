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

  private colorService = inject(ColorService);

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
      issueTypeColor = this.colorService.colorNameToRgba(issueTypeColor);

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
}
