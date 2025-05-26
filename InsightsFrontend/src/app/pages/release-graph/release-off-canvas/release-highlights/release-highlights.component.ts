import {
  AfterViewInit, Component, ElementRef, Input,
  OnChanges, OnDestroy, ViewChild, ChangeDetectorRef
} from '@angular/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { Label } from '../../../../services/label.service';
import { Issue } from '../../../../services/issue.service';

@Component({
  selector: 'app-release-highlights',
  standalone: true,
  imports: [NgxChartsModule],
  templateUrl: './release-highlights.component.html',
  styleUrl: './release-highlights.component.scss',
})
export class ReleaseHighlightsComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('chartWrapper', { static: true }) chartWrapper!: ElementRef<HTMLDivElement>;
  @Input() highlightedLabels: Label[] | undefined;
  @Input() releaseIssues: Issue[] | undefined;

  public pieWidth = 200;
  public pieData: { name: string; value: number; color: string }[] = [];
  public customColors: { name: string; value: string }[] = [];

  private resizeObserver!: ResizeObserver;

  constructor(private cdr: ChangeDetectorRef) {
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.handleResize();
      this.cdr.detectChanges();
    });

    this.resizeObserver = new ResizeObserver(() => {
      this.handleResize();
      this.cdr.detectChanges();
    });
    this.resizeObserver.observe(this.chartWrapper.nativeElement);
  }

  ngOnChanges(): void {
    if (this.releaseIssues) {
      this.generatePieData();
    }
  }

  ngOnDestroy(): void {
    this.resizeObserver?.disconnect();
  }

  public getDotColor(color: string): string {
    return color.startsWith('#') ? color : `#${color}`;
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

    this.pieData = [...pieDataMap.entries()].map(([name, info]) => ({
      name,
      value: info.count,
      color: info.color,
    }));

    this.customColors = this.pieData.map((d) => ({ name: d.name, value: d.color }));
  }

  private colorNameToRgba(color: string, alpha: number): string {
    const temporaryElement = document.createElement('div');
    temporaryElement.style.color = color;
    document.body.append(temporaryElement);

    const rgb = getComputedStyle(temporaryElement).color;
    temporaryElement.remove();

    const match = rgb.match(/^rgb[a]?\((\d+),\s*(\d+),\s*(\d+)/i);
    if (match) {
      const [, r, g, b] = match;
      return `rgba(${r},${g},${b},${alpha})`;
    }

    return color;
  }

  private handleResize(): void {
    const wrapper = this.chartWrapper.nativeElement;
    const w = wrapper.offsetWidth || 200;
    this.pieWidth = Math.max(140, Math.min(350, Math.round(w)));
  }
}
