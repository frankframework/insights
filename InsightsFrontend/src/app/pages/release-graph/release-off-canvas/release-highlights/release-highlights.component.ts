import { Component, Input, OnChanges } from '@angular/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ReleaseHighlights } from '../../../../services/label.service';

@Component({
  selector: 'app-release-highlights',
  standalone: true,
  imports: [NgxChartsModule],
  templateUrl: './release-highlights.component.html',
  styleUrl: './release-highlights.component.scss',
})
export class ReleaseHighlightsComponent implements OnChanges {
  @Input() highlights: ReleaseHighlights | undefined;

  public pieData: { name: string; value: number; color: string }[] = [];
  public customColors: { name: string; value: string }[] = [];

  ngOnChanges(): void {
    if (this.highlights) {
      this.generatePieData();
    }
  }

  private generatePieData(): void {
    if (!this.highlights) return;

    const { AllHighlights } = this.highlights;

    const colorMap = new Map<string, string>();

    this.pieData = Object.entries(AllHighlights).map(([rawKey, count]) => {
      const nameMatch = rawKey.match(/name=([^,]+)/);
      const colorMatch = rawKey.match(/color=([^,\]]+)/);

      const name = nameMatch ? nameMatch[1] : rawKey;
      const keyColor = colorMatch ? `#${colorMatch[1]}` : '#888';

      const color = colorMap.get(name) ?? keyColor;

      return { name, value: count as number, color };
    });

    this.customColors = this.pieData.map((d) => ({ name: d.name, value: d.color }));
  }
}
