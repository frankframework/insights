import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { SkipNode } from '../../../pages/release-graph/release-link.service';
import { ReleaseNode, ReleaseNodeService } from '../../../pages/release-graph/release-node.service';

interface ReleaseTreeNode {
  release: any;
  version: string;
  type: 'major' | 'minor' | 'patch';
  patches: any[];
}

@Component({
  selector: 'app-skipped-versions-modal',
  standalone: true,
  imports: [CommonModule, ModalComponent],
  templateUrl: './release-skipped-versions.html',
  styleUrls: ['./release-skipped-versions.scss'],
})
export class ReleaseSkippedVersions implements OnChanges {
  @Input() skipNode: SkipNode | null = null;
  @Input() releases: any[] = [];
  @Output() closed = new EventEmitter<void>();
  @Output() versionClicked = new EventEmitter<string>();

  public releaseTree: ReleaseTreeNode[] = [];

  private nodeService = inject(ReleaseNodeService);

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['skipNode'] || changes['releases']) && this.skipNode && this.releases.length > 0) {
      this.structureSkippedReleases();
    }
  }

  private structureSkippedReleases(): void {
    if (!this.skipNode) return;

    const skippedReleases = this.releases.filter(release => {
      const versionName = release.name.startsWith('v') ? release.name : `v${release.name}`;
      return this.skipNode!.skippedVersions.includes(versionName);
    });

    const releaseMap = new Map<string, ReleaseTreeNode>();

    skippedReleases.forEach(release => {
      const info = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!info) return;

      if (info.type === 'major' || info.type === 'minor') {
        const versionKey = release.name.startsWith('v') ? release.name : `v${release.name}`;
        releaseMap.set(versionKey, {
          release,
          version: versionKey,
          type: info.type,
          patches: []
        });
      } else if (info.type === 'patch') {
        const parentKey = `v${info.major}.${info.minor}`;
        const parent = releaseMap.get(parentKey);
        if (parent) {
          parent.patches.push(release);
        } else {
          const versionKey = release.name.startsWith('v') ? release.name : `v${release.name}`;
          releaseMap.set(parentKey, {
            release: null,
            version: parentKey,
            type: 'minor',
            patches: [release]
          });
        }
      }
    });

    this.releaseTree = Array.from(releaseMap.values()).sort((a, b) => {
      const infoA = this.nodeService.getVersionInfo({ label: a.version } as ReleaseNode);
      const infoB = this.nodeService.getVersionInfo({ label: b.version } as ReleaseNode);
      if (!infoA || !infoB) return 0;
      if (infoA.major !== infoB.major) return infoA.major - infoB.major;
      return infoA.minor - infoB.minor;
    });

    this.releaseTree.forEach(node => {
      node.patches.sort((a, b) => {
        const infoA = this.nodeService.getVersionInfo({ label: a.name } as ReleaseNode);
        const infoB = this.nodeService.getVersionInfo({ label: b.name } as ReleaseNode);
        if (!infoA || !infoB) return 0;
        return infoA.patch - infoB.patch;
      });
    });
  }

  onVersionClick(version: string): void {
    this.versionClicked.emit(version);
  }

  closeModal(): void {
    this.closed.emit();
  }
}
