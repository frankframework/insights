import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { SkipNode } from '../release-link.service';
import { ReleaseNode, ReleaseNodeService } from '../release-node.service';

interface Release {
  id: string;
  name: string;
  branch: { name: string };
}

interface ReleaseTreeNode {
  release: Release | null;
  version: string;
  type: 'major' | 'minor' | 'patch';
  patches: Release[];
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
  @Input() releases: Release[] = [];
  @Output() closed = new EventEmitter<void>();
  @Output() versionClicked = new EventEmitter<string>();

  public releaseTree: ReleaseTreeNode[] = [];

  private nodeService = inject(ReleaseNodeService);

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['skipNode'] || changes['releases']) && this.skipNode && this.releases.length > 0) {
      this.structureSkippedReleases();
    }
  }

  public onVersionClick(version: string): void {
    this.versionClicked.emit(version);
  }

  public closeModal(): void {
    this.closed.emit();
  }

  private structureSkippedReleases(): void {
    if (!this.skipNode) return;

    const skippedReleases = this.getSkippedReleases();
    const releaseMap = this.buildReleaseMap(skippedReleases);
    this.releaseTree = this.sortReleaseTree(releaseMap);
    this.sortPatchesInTree();
  }

  private getSkippedReleases(): Release[] {
    return this.releases.filter((release) => {
      const versionName = release.name.startsWith('v') ? release.name : `v${release.name}`;
      return this.skipNode!.skippedVersions.includes(versionName);
    });
  }

  private buildReleaseMap(skippedReleases: Release[]): Map<string, ReleaseTreeNode> {
    const releaseMap = new Map<string, ReleaseTreeNode>();

    for (const release of skippedReleases) {
      const info = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!info) continue;

      if (info.type === 'major' || info.type === 'minor') {
        this.handleMajorOrMinorRelease(release, info, releaseMap);
      } else if (info.type === 'patch') {
        this.handlePatchRelease(release, info, releaseMap);
      }
    }

    return releaseMap;
  }

  private handleMajorOrMinorRelease(
    release: Release,
    info: { type: 'major' | 'minor' | 'patch' },
    releaseMap: Map<string, ReleaseTreeNode>,
  ): void {
    const versionKey = release.name.startsWith('v') ? release.name : `v${release.name}`;
    releaseMap.set(versionKey, {
      release,
      version: versionKey,
      type: info.type,
      patches: [],
    });
  }

  private handlePatchRelease(
    release: Release,
    info: { major: number; minor: number },
    releaseMap: Map<string, ReleaseTreeNode>,
  ): void {
    const parentKey = `v${info.major}.${info.minor}`;
    const parent = releaseMap.get(parentKey);

    if (parent) {
      parent.patches.push(release);
    } else {
      releaseMap.set(parentKey, {
        release: null,
        version: parentKey,
        type: 'minor',
        patches: [release],
      });
    }
  }

  private sortReleaseTree(releaseMap: Map<string, ReleaseTreeNode>): ReleaseTreeNode[] {
    return [...releaseMap.values()].toSorted((a, b) => {
      const infoA = this.nodeService.getVersionInfo({ label: a.version } as ReleaseNode);
      const infoB = this.nodeService.getVersionInfo({ label: b.version } as ReleaseNode);
      if (!infoA || !infoB) return 0;
      if (infoA.major !== infoB.major) return infoA.major - infoB.major;
      return infoA.minor - infoB.minor;
    });
  }

  private sortPatchesInTree(): void {
    for (const node of this.releaseTree) {
      node.patches.sort((a, b) => {
        const infoA = this.nodeService.getVersionInfo({ label: a.name } as ReleaseNode);
        const infoB = this.nodeService.getVersionInfo({ label: b.name } as ReleaseNode);
        if (!infoA || !infoB) return 0;
        return infoA.patch - infoB.patch;
      });
    }
  }
}
