import { Component, Input, inject } from '@angular/core';
import { Issue } from '../../../../../services/issue.service';
import { ReleaseOffCanvasComponent } from '../../release-off-canvas.component';

@Component({
  selector: 'app-issue-tree-branch',
  standalone: true,
  templateUrl: './issue-tree-branch.component.html',
  styleUrl: './issue-tree-branch.component.scss',
})
export class IssueTreeBranchComponent {
  private static readonly MAX_SUB_ISSUE_DEPTH = 8;

  @Input() issue!: Issue;
  @Input() depth = 0;

  protected expanded = false;

  private releaseOffCanvasComponent = inject(ReleaseOffCanvasComponent);

  public toggleExpand(): void {
    this.expanded = !this.expanded;
  }

  public getIndent(): string {
    const d = Math.min(this.depth, IssueTreeBranchComponent.MAX_SUB_ISSUE_DEPTH);
    return `${d}rem`;
  }

  public getTypeTextColor(issueType?: { color?: string }): string {
    if (!issueType?.color) return 'white';
    const rgba = this.releaseOffCanvasComponent.colorNameToRgba(issueType.color.trim().toLowerCase());

    const match = rgba.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)/i);
    if (!match) return 'white';

    const r = Number.parseInt(match[1], 10);
    const g = Number.parseInt(match[2], 10);
    const b = Number.parseInt(match[3], 10);

    const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
    return luminance > 0.7 ? 'black' : 'white';
  }
}
