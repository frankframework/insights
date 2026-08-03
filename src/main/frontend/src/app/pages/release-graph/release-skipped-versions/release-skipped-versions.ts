import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalComponent } from '../../../components/modal/modal.component';
import { Release } from '../../../services/release.service';
import { SkipNode } from '../release-link.service';
import { ReleaseNode, ReleaseNodeService } from '../release-node.service';
import {
  areRangesCovered,
  createExactVersionRanges,
  createReleaseLineRanges,
  serializeVersionRanges,
  VersionRange,
} from '../../../pipes/release-range';
import { IncludeVersionButtonComponent } from './include-version-button/include-version-button.component';
import { IncludeActionsComponent } from './include-actions/include-actions.component';

interface ReleaseTreeNode {
  release: Release | null;
  version: string;
  type: 'major' | 'minor' | 'patch';
  patches: Release[];
}

@Component({
  selector: 'app-skipped-versions-modal',
  standalone: true,
  imports: [CommonModule, ModalComponent, IncludeVersionButtonComponent, IncludeActionsComponent],
  templateUrl: './release-skipped-versions.html',
  styleUrls: ['./release-skipped-versions.scss'],
})
export class ReleaseSkippedVersions implements OnChanges {
  @Input() skipNode: SkipNode | null = null;
  @Input() releases: Release[] = [];
  @Input() releaseRanges: VersionRange[] = [];
  @Output() closed = new EventEmitter<void>();
  @Output() versionClicked = new EventEmitter<string>();
  @Output() rangeRequested = new EventEmitter<VersionRange[]>();

  public releaseTree: ReleaseTreeNode[] = [];
  public pendingVersions: string[] = [];

  private nodeService = inject(ReleaseNodeService);

  public get includableRanges(): VersionRange[] {
    return createReleaseLineRanges(this.releaseTree.map((node) => node.version));
  }

  public get includeLabel(): string {
    return serializeVersionRanges(this.includableRanges);
  }

  public get pendingRanges(): VersionRange[] {
    return createExactVersionRanges(this.pendingVersions);
  }

  public get isAlreadyIncluded(): boolean {
    return areRangesCovered(this.releaseRanges, this.includableRanges);
  }

  public get hasPending(): boolean {
    return this.pendingVersions.length > 0;
  }

  public isVersionIncluded(version: string): boolean {
    return areRangesCovered(this.releaseRanges, createExactVersionRanges([version]));
  }

  public isVersionPending(version: string): boolean {
    return this.pendingVersions.includes(version);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['skipNode'] || changes['releases']) && this.skipNode && this.releases.length > 0) {
      this.pendingVersions = [];
      this.structureSkippedReleases();
    }
  }

  public onVersionClick(version: string): void {
    this.versionClicked.emit(version);
  }

  public includeReleases(): void {
    const ranges = this.includableRanges;
    if (ranges.length === 0) return;

    this.rangeRequested.emit(ranges);
  }

  public includeVersion(version: string): void {
    const isRelease = createExactVersionRanges([version]).length > 0;
    if (!isRelease || this.pendingVersions.includes(version)) return;

    this.pendingVersions = [...this.pendingVersions, version];
  }

  public removeFromPending(version: string): void {
    this.pendingVersions = this.pendingVersions.filter((pending) => pending !== version);
  }

  public applyPendingIncludes(): void {
    const ranges = this.pendingRanges;
    if (ranges.length === 0) return;

    this.rangeRequested.emit(ranges);
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
