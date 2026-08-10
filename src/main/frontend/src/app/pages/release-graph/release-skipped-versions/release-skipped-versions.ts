import { ChangeDetectionStrategy, Component, Signal, computed, inject, input, output } from '@angular/core';
import { ModalComponent } from '../../../components/modal/modal.component';
import { Release } from '../../../services/release.service';
import { SkipNode } from '../release-link.service';
import { ReleaseNode, ReleaseNodeService } from '../release-node.service';

interface ReleaseTreeNode {
  release: Release | null;
  version: string;
  type: 'major' | 'minor' | 'patch';
  patches: Release[];
}

@Component({
  selector: 'app-skipped-versions-modal',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './release-skipped-versions.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrls: ['./release-skipped-versions.scss'],
})
export class ReleaseSkippedVersions {
  public readonly skipNode = input<SkipNode | null>(null);
  public readonly releases = input<Release[]>([]);
  public readonly closed = output<void>();
  public readonly versionClicked = output<string>();

  public readonly releaseTree: Signal<ReleaseTreeNode[]> = computed(() => {
    const skipNode = this.skipNode();
    const releases = this.releases();
    if (!skipNode || releases.length === 0) return [];

    const releaseMap = this.buildReleaseMap(ReleaseSkippedVersions.getSkippedReleases(skipNode, releases));
    return this.sortPatchesInTree(this.sortReleaseTree(releaseMap));
  });

  private readonly nodeService = inject(ReleaseNodeService);

  private static isNightlyRelease(label: string): boolean {
    if (label.toLowerCase().includes('snapshot')) {
      return true;
    }

    const timestampPattern = /^v?\d+\.\d+\.\d+-\d{8}\.\d{6}/;
    return timestampPattern.test(label);
  }

  private static getSkippedReleases(skipNode: SkipNode, releases: Release[]): Release[] {
    return releases.filter((release) => {
      const versionName = release.name.startsWith('v') ? release.name : `v${release.name}`;
      return skipNode.skippedVersions.includes(versionName) && !ReleaseSkippedVersions.isNightlyRelease(release.name);
    });
  }

  private static handlePatchRelease(
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

  private static handleMajorOrMinorRelease(
    release: Release,
    info: { type: 'major' | 'minor' | 'patch'; major: number; minor: number },
    releaseMap: Map<string, ReleaseTreeNode>,
  ): void {
    const prefixedReleaseName = release.name.startsWith('v') ? release.name : `v${release.name}`;
    const mapKey = `v${info.major}.${info.minor}`;
    const existingNode = releaseMap.get(mapKey);

    releaseMap.set(mapKey, {
      release,
      version: prefixedReleaseName,
      type: info.type,
      patches: existingNode ? existingNode.patches : [],
    });
  }

  public onVersionClick(version: string): void {
    this.versionClicked.emit(version);
  }

  public closeModal(): void {
    this.closed.emit();
  }

  private buildReleaseMap(skippedReleases: Release[]): Map<string, ReleaseTreeNode> {
    const releaseMap = new Map<string, ReleaseTreeNode>();

    for (const release of skippedReleases) {
      const info = this.nodeService.getVersionInfo({ label: release.name } as ReleaseNode);
      if (!info) continue;

      if (info.type === 'major' || info.type === 'minor') {
        ReleaseSkippedVersions.handleMajorOrMinorRelease(release, info, releaseMap);
      } else if (info.type === 'patch') {
        ReleaseSkippedVersions.handlePatchRelease(release, info, releaseMap);
      }
    }

    return releaseMap;
  }

  private sortReleaseTree(releaseMap: Map<string, ReleaseTreeNode>): ReleaseTreeNode[] {
    return [...releaseMap.values()].toSorted((nodeA, nodeB) => {
      const infoA = this.nodeService.getVersionInfo({ label: nodeA.version } as ReleaseNode);
      const infoB = this.nodeService.getVersionInfo({ label: nodeB.version } as ReleaseNode);
      if (!infoA || !infoB) return 0;
      if (infoA.major !== infoB.major) return infoA.major - infoB.major;
      return infoA.minor - infoB.minor;
    });
  }

  private sortPatchesInTree(releaseTree: ReleaseTreeNode[]): ReleaseTreeNode[] {
    for (const node of releaseTree) {
      node.patches.sort((releaseA, releaseB) => {
        const infoA = this.nodeService.getVersionInfo({ label: releaseA.name } as ReleaseNode);
        const infoB = this.nodeService.getVersionInfo({ label: releaseB.name } as ReleaseNode);
        if (!infoA || !infoB) return 0;
        return infoA.patch - infoB.patch;
      });
    }

    return releaseTree;
  }
}
