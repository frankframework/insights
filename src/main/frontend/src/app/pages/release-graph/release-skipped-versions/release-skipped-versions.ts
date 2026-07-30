import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { Release } from '../../../services/release.service';
import { SkipNode } from '../release-link.service';
import { ReleaseNode, ReleaseNodeService } from '../release-node.service';
import {
  areRangesIncluded,
  createExactVersionRanges,
  createReleaseLineRanges,
  IncludeRange,
  mergeIncludeRanges,
  serializeIncludeRanges,
} from '../../../pipes/release-include';

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
  @Input() includedReleases: IncludeRange[] = [];
  @Output() closed = new EventEmitter<void>();
  @Output() versionClicked = new EventEmitter<string>();
  @Output() includeRequested = new EventEmitter<IncludeRange[]>();

  public releaseTree: ReleaseTreeNode[] = [];
  public pendingRanges: IncludeRange[] = [];

  private nodeService = inject(ReleaseNodeService);

  public get includableRanges(): IncludeRange[] {
    return createReleaseLineRanges(this.releaseTree.map((node) => node.version));
  }

  public get includeLabel(): string {
    return serializeIncludeRanges(this.includableRanges);
  }

  public get isAlreadyIncluded(): boolean {
    return areRangesIncluded(this.includedReleases, this.includableRanges);
  }

  public get hasPending(): boolean {
    return this.pendingRanges.length > 0;
  }

  public isVersionIncluded(version: string): boolean {
    return areRangesIncluded(this.includedReleases, createExactVersionRanges([version]));
  }

  public isVersionPending(version: string): boolean {
    return areRangesIncluded(this.pendingRanges, createExactVersionRanges([version]));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['skipNode'] || changes['releases']) && this.skipNode && this.releases.length > 0) {
      this.pendingRanges = [];
      this.structureSkippedReleases();
    }
  }

  public onVersionClick(version: string): void {
    this.versionClicked.emit(version);
  }

  public includeReleases(): void {
    const ranges = this.includableRanges;
    if (ranges.length === 0) return;

    this.includeRequested.emit(ranges);
  }

  public includeVersion(version: string): void {
    const ranges = createExactVersionRanges([version]);
    if (ranges.length === 0) return;

    this.pendingRanges = mergeIncludeRanges(this.pendingRanges, ranges);
  }

  public removeFromPending(version: string): void {
    const ranges = createExactVersionRanges([version]);
    if (ranges.length === 0) return;

    this.pendingRanges = this.pendingRanges.filter(
      (r) =>
        !ranges.some(
          (rem) =>
            rem.from.major === r.from.major &&
            rem.from.minor === r.from.minor &&
            rem.from.patch === r.from.patch &&
            rem.to.major === r.to.major &&
            rem.to.minor === r.to.minor &&
            rem.to.patch === r.to.patch,
        ),
    );
  }

  public applyPendingIncludes(): void {
    if (this.pendingRanges.length === 0) return;
    this.includeRequested.emit(this.pendingRanges);
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
      const isIncluded = this.skipNode!.skippedVersions.includes(versionName);
      const isNightly = this.isNightlyRelease(release.name);
      return isIncluded && !isNightly;
    });
  }

  private isNightlyRelease(label: string): boolean {
    if (label.toLowerCase().includes('snapshot')) {
      return true;
    }

    const timestampPattern = /^v?\d+\.\d+\.\d+-\d{8}\.\d{6}/;
    return timestampPattern.test(label);
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
    info: { type: 'major' | 'minor' | 'patch'; major: number; minor: number },
    releaseMap: Map<string, ReleaseTreeNode>,
  ): void {
    const prefixedReleaseName = release.name.startsWith('v') ? release.name : `v${release.name}`;

    const mapKey = `v${info.major}.${info.minor}`;

    const displayVersion = prefixedReleaseName;
    const existingNode = releaseMap.get(mapKey);

    releaseMap.set(mapKey, {
      release,
      version: displayVersion,
      type: info.type,
      patches: existingNode ? existingNode.patches : [],
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
