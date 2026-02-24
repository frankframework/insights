import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReleaseLinkService, SkipNode } from './release-link.service';
import { ReleaseNode, ReleaseNodeService } from './release-node.service';
import { Release } from '../../services/release.service';

const MASTER_BRANCH_NAME = 'master';

const createMockNode = (id: string, originalBranch?: string, label?: string): ReleaseNode => ({
  id,
  label: label ?? id,
  branch: originalBranch ?? MASTER_BRANCH_NAME,
  originalBranch,
  position: { x: 0, y: 0 },
  color: '',
  publishedAt: new Date(),
});

describe('ReleaseLinkService', () => {
  let service: ReleaseLinkService;
  let mockNodeService: jasmine.SpyObj<ReleaseNodeService>;

  const masterNode1 = createMockNode('master-1', undefined, 'v10.0.0');
  const masterNode3 = createMockNode('master-3', undefined, 'v11.0.0');

  const subNode8_1 = createMockNode('sub-8.0-1', 'release/8.0', 'v8.0.0');
  const subNode8_2 = createMockNode('sub-8.0-2', 'release/8.0', 'v8.0.1');

  const subNode9_1 = createMockNode('sub-9.0-1', 'release/9.0', 'v9.0.0');

  const miniNode8: ReleaseNode = {
    ...createMockNode(`mini-${subNode8_1.id}`, 'master', subNode8_1.label),
    isMiniNode: true,
    originalBranch: subNode8_1.branch,
    linkedBranchNode: subNode8_1.id,
  };
  const miniNode9: ReleaseNode = {
    ...createMockNode(`mini-${subNode9_1.id}`, 'master', subNode9_1.label),
    isMiniNode: true,
    originalBranch: subNode9_1.branch,
    linkedBranchNode: subNode9_1.id,
  };

  beforeEach(() => {
    const nodeServiceSpy = jasmine.createSpyObj('ReleaseNodeService', ['getVersionInfo', 'timelineScale']);

    nodeServiceSpy.getVersionInfo.and.callFake((node: ReleaseNode) => {
      const match = node.label.match(/^v?(\d+)\.(\d+)(?:\.(\d+))?/i);
      if (!match) return null;
      const major = Number.parseInt(match[1], 10);
      const minor = Number.parseInt(match[2], 10);
      const patch = Number.parseInt(match[3] ?? '0', 10);
      let type: 'major' | 'minor' | 'patch' = 'patch';
      if (patch === 0 && minor > 0) type = 'minor';
      else if (patch === 0 && minor === 0) type = 'major';
      return { major, minor, patch, type };
    });

    TestBed.configureTestingModule({
      providers: [
        ReleaseLinkService,
        { provide: ReleaseNodeService, useValue: nodeServiceSpy },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ReleaseLinkService);
    mockNodeService = TestBed.inject(ReleaseNodeService) as jasmine.SpyObj<ReleaseNodeService>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createSkipNodes() - Core Functionality', () => {
    it('should return an empty array if the skipnodes input structuredGroups is empty', () => {
      const skipNodes = service.createSkipNodes([], []);

      expect(skipNodes).toEqual([]);
    });

    it('should return an empty array if master branch is missing', () => {
      const structuredGroups = [new Map([['release/9.0', [subNode9_1]]])];
      const skipNodes = service.createSkipNodes(structuredGroups, []);

      expect(skipNodes).toEqual([]);
    });

    it('should return an empty array if master branch has no nodes', () => {
      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, []]])];
      const skipNodes = service.createSkipNodes(structuredGroups, []);

      expect(skipNodes).toEqual([]);
    });

    it('should create skip nodes for version gaps in master branch', () => {
      const masterNodes = [
        createMockNode('v6.0.0', undefined, 'v6.0.0'),
        createMockNode('v8.0.0', undefined, 'v8.0.0')
      ];
      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, masterNodes]])];
      const skippedReleases: Release[] = [
        {
          id: 'v7.0.0',
          name: 'v7.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v7.1.0',
          name: 'v7.1.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];

      masterNodes[0].position = { x: 0, y: 0 };
      masterNodes[1].position = { x: 450, y: 0 };

      const skipNodes = service.createSkipNodes(structuredGroups, skippedReleases);

      expect(skipNodes.length).toBe(1);
      expect(skipNodes[0].id).toBe('skip-v6.0.0-v8.0.0');
      expect(skipNodes[0].x).toBe(225);
      expect(skipNodes[0].y).toBe(0);
      expect(skipNodes[0].skippedCount).toBe(2);
      expect(skipNodes[0].skippedVersions).toEqual(['v7.0.0', 'v7.1.0']);
    });

    it('should create initial skip node for releases before the first master node', () => {
      const masterNodes = [createMockNode('v5.0.0', undefined, 'v5.0.0')];
      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, masterNodes]])];
      const skippedReleases: Release[] = [
        {
          id: 'v3.0.0',
          name: 'v3.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v4.0.0',
          name: 'v4.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];

      masterNodes[0].position = { x: 450, y: 0 };

      mockNodeService.timelineScale = { pixelsPerDay: 1 } as any;

      const skipNodes = service.createSkipNodes(structuredGroups, skippedReleases);

      expect(skipNodes.length).toBe(1);
      expect(skipNodes[0].id).toBe('skip-initial-v5.0.0');
      expect(skipNodes[0].x).toBe(225);
      expect(skipNodes[0].y).toBe(0);
      expect(skipNodes[0].skippedCount).toBe(2);
      expect(skipNodes[0].skippedVersions).toEqual(['v3.0.0', 'v4.0.0']);
    });

    it('should only count minor/major releases in skip nodes, excluding patch versions', () => {
      const masterNodes = [
        createMockNode('v6.0.0', undefined, 'v6.0.0'),
        createMockNode('v8.0.0', undefined, 'v8.0.0')
      ];
      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, masterNodes]])];
      const skippedReleases: Release[] = [
        {
          id: 'v7.0.0',
          name: 'v7.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v7.0.1',
          name: 'v7.0.1',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v7.1.0',
          name: 'v7.1.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v7.1.1',
          name: 'v7.1.1',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];

      masterNodes[0].position = { x: 0, y: 0 };
      masterNodes[1].position = { x: 450, y: 0 };

      const skipNodes = service.createSkipNodes(structuredGroups, skippedReleases);

      expect(skipNodes.length).toBe(1);
      expect(skipNodes[0].skippedCount).toBe(2);
      expect(skipNodes[0].label).toBe('2 skipped');
    });

    it('should not create skip node if no minor/major releases are skipped', () => {
      const masterNodes = [
        createMockNode('v6.0.0', undefined, 'v6.0.0'),
        createMockNode('v6.1.0', undefined, 'v6.1.0')
      ];
      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, masterNodes]])];
      const skippedReleases: Release[] = [
        {
          id: 'v6.0.1',
          name: 'v6.0.1',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v6.0.2',
          name: 'v6.0.2',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];

      masterNodes[0].position = { x: 0, y: 0 };
      masterNodes[1].position = { x: 250, y: 0 };

      const skipNodes = service.createSkipNodes(structuredGroups, skippedReleases);

      expect(skipNodes.length).toBe(0);
    });
  });

  describe('createSkipNodeLinks() - Link Generation', () => {
    it('should create links for initial skip nodes', () => {
      const masterNodes = [createMockNode('v5.0.0', undefined, 'v5.0.0')];
      const skipNodes: SkipNode[] = [{
        id: 'skip-initial-v5.0.0',
        x: 225,
        y: 0,
        skippedCount: 2,
        skippedVersions: ['v3.0.0', 'v4.0.0'],
        label: '2 skipped'
      }];

      const links = service.createSkipNodeLinks(skipNodes, masterNodes);

      expect(links.length).toBe(1);
      expect(links[0].source).toBe('skip-initial-v5.0.0');
      expect(links[0].target).toBe('v5.0.0');
      expect(links[0].isGap).toBe(true);
    });

    it('should create two links for regular skip nodes (source to skip, skip to target)', () => {
      const masterNodes = [
        createMockNode('v6.0.0', undefined, 'v6.0.0'),
        createMockNode('v8.0.0', undefined, 'v8.0.0')
      ];
      masterNodes[0].position = { x: 0, y: 0 };
      masterNodes[1].position = { x: 450, y: 0 };

      const skipNodes: SkipNode[] = [{
        id: 'skip-v6.0.0-v8.0.0',
        x: 225,
        y: 0,
        skippedCount: 1,
        skippedVersions: ['v7.0.0'],
        label: '1 skipped',
        sourceNodeId: 'v6.0.0',
        targetNodeId: 'v8.0.0',
      }];

      const links = service.createSkipNodeLinks(skipNodes, masterNodes);

      expect(links.length).toBe(2);
      expect(links[0].source).toBe('v6.0.0');
      expect(links[0].target).toBe('skip-v6.0.0-v8.0.0');
      expect(links[0].isGap).toBe(true);
      expect(links[1].source).toBe('skip-v6.0.0-v8.0.0');
      expect(links[1].target).toBe('v8.0.0');
      expect(links[1].isGap).toBe(true);
    });

    it('should return empty array if no skip nodes provided', () => {
      const masterNodes = [createMockNode('v5.0.0')];
      const links = service.createSkipNodeLinks([], masterNodes);

      expect(links).toEqual([]);
    });

    it('should handle skip nodes when master nodes array is empty', () => {
      const skipNodes: SkipNode[] = [{
        id: 'skip-initial-v5.0.0',
        x: 225,
        y: 0,
        skippedCount: 1,
        skippedVersions: ['v4.0.0'],
        label: '1 skipped'
      }];

      const links = service.createSkipNodeLinks(skipNodes, []);

      expect(links).toEqual([]);
    });
  });

  describe('createLinks() - Edge Cases', () => {
    it('should return an empty array if the links input structuredGroups is empty', () => {
      const links = service.createLinks([], []);

      expect(links).toEqual([]);
    });

    it('should create a direct link between major-version nodes when no skip node covers the gap', () => {
      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, [masterNode1, masterNode3]]])];
      const links = service.createLinks(structuredGroups, []);

      const directLink = links.find((link) => link.source === masterNode1.id && link.target === masterNode3.id);

      expect(directLink).toBeDefined();

      const specialLinks = links.filter((link) => link.source.startsWith('start-node-'));

      expect(specialLinks.length).toBe(1);
      expect(specialLinks[0].source).toBe('start-node-master-1');
    });

    it('should not crash and return an empty array if master branch is missing or empty', () => {
      const noMasterGroups = [new Map([['release/9.0', [subNode9_1]]])];
      const links1 = service.createLinks(noMasterGroups, []);

      expect(links1).toEqual([]);

      const emptyMasterGroups = [new Map([[MASTER_BRANCH_NAME, []]])];
      const links2 = service.createLinks(emptyMasterGroups, []);

      expect(links2).toEqual([]);
    });
  });

  describe('Core Linking Logic', () => {
    let masterNodes: ReleaseNode[];
    let structuredGroups: Map<string, ReleaseNode[]>[];

    beforeEach(() => {
      masterNodes = [masterNode1, miniNode8, miniNode9, masterNode3];
      structuredGroups = [
        new Map([[MASTER_BRANCH_NAME, masterNodes]]),
        new Map([['release/8.0', [subNode8_1, subNode8_2]]]),
        new Map([['release/9.0', [subNode9_1]]]),
      ];
    });

    it('should create a correct anchor link from a master node to a sub-branch node', () => {
      const links = service.createLinks(structuredGroups, []);
      const anchorLink = links.find((link) => link.source === miniNode8.id && link.target === subNode8_1.id);

      expect(anchorLink).toBeDefined();
      expect(anchorLink?.target).toBe('sub-8.0-1');
    });

    it('should create correct intra-branch links for a sub-branch', () => {
      const links = service.createLinks(structuredGroups, []);
      const subBranchLink = links.find((link) => link.source === 'sub-8.0-1');

      expect(subBranchLink).toBeDefined();
      expect(subBranchLink?.target).toBe('sub-8.0-2');
    });

    it('should NOT create an anchor link if its anchor node is missing from the master list', () => {
      const incompleteMasterNodes = [masterNode1, miniNode8, masterNode3];
      const groups = [new Map([[MASTER_BRANCH_NAME, incompleteMasterNodes]]), new Map([['release/9.0', [subNode9_1]]])];
      const links = service.createLinks(groups, []);

      const missingAnchorLink = links.find((link) => link.source === miniNode9.id);

      expect(missingAnchorLink).toBeUndefined();
    });

    it('should include special fade-in links when skip nodes are provided', () => {
      const skipNodes: SkipNode[] = [{
        id: 'skip-initial-master-1',
        x: 100,
        y: 0,
        skippedCount: 1,
        skippedVersions: ['v1.0.0'],
        label: '1 skipped'
      }];

      const links = service.createLinks(structuredGroups, skipNodes);
      const fadeInLink = links.find((link) => link.isGap && link.source.startsWith('start-node-'));

      expect(fadeInLink).toBeDefined();
      expect(fadeInLink?.isGap).toBe(true);
      expect(fadeInLink?.source).toBe('start-node-master-1');
      expect(fadeInLink?.target).toBe('skip-initial-master-1');
    });
  });

  describe('Regression: direct link between consecutive mini nodes across a major version boundary', () => {
    it('should create a direct link between the 9.4 and 10.0 mini nodes when no intermediate releases are skipped', () => {
      const subNode94: ReleaseNode = {
        id: 'sub-9.4-1',
        label: 'v9.4.0',
        branch: 'release/9.4',
        position: { x: 100, y: 90 },
        color: '',
        publishedAt: new Date('2023-06-01'),
      };

      const subNode100: ReleaseNode = {
        id: 'sub-10.0-1',
        label: 'v10.0.0',
        branch: 'release/10.0',
        position: { x: 200, y: 90 },
        color: '',
        publishedAt: new Date('2024-01-01'),
      };

      const miniNode94: ReleaseNode = {
        id: 'mini-sub-9.4-1',
        label: '',
        branch: MASTER_BRANCH_NAME,
        originalBranch: 'release/9.4',
        position: { x: 60, y: 0 },
        color: '',
        publishedAt: new Date('2023-06-01'),
        isMiniNode: true,
        linkedBranchNode: subNode94.id,
      };

      const miniNode100: ReleaseNode = {
        id: 'mini-sub-10.0-1',
        label: '',
        branch: MASTER_BRANCH_NAME,
        originalBranch: 'release/10.0',
        position: { x: 160, y: 0 },
        color: '',
        publishedAt: new Date('2024-01-01'),
        isMiniNode: true,
        linkedBranchNode: subNode100.id,
      };

      const structuredGroups = [
        new Map([[MASTER_BRANCH_NAME, [miniNode94, miniNode100]]]),
        new Map([['release/9.4', [subNode94]]]),
        new Map([['release/10.0', [subNode100]]]),
      ];

      const links = service.createLinks(structuredGroups, []);

      const directLink = links.find(
        (link) => link.source === miniNode94.id && link.target === miniNode100.id,
      );

      expect(directLink).withContext('expected a direct link between the 9.4 and 10.0 mini nodes').toBeDefined();
    });

    it('should NOT create a direct link between mini nodes when a skip node already covers the gap', () => {
      const subNode94: ReleaseNode = {
        id: 'sub-9.4-1',
        label: 'v9.4.0',
        branch: 'release/9.4',
        position: { x: 100, y: 90 },
        color: '',
        publishedAt: new Date('2023-06-01'),
      };

      const subNode100: ReleaseNode = {
        id: 'sub-10.0-1',
        label: 'v10.0.0',
        branch: 'release/10.0',
        position: { x: 200, y: 90 },
        color: '',
        publishedAt: new Date('2024-01-01'),
      };

      const miniNode94: ReleaseNode = {
        id: 'mini-sub-9.4-1',
        label: '',
        branch: MASTER_BRANCH_NAME,
        originalBranch: 'release/9.4',
        position: { x: 60, y: 0 },
        color: '',
        publishedAt: new Date('2023-06-01'),
        isMiniNode: true,
        linkedBranchNode: subNode94.id,
      };

      const miniNode100: ReleaseNode = {
        id: 'mini-sub-10.0-1',
        label: '',
        branch: MASTER_BRANCH_NAME,
        originalBranch: 'release/10.0',
        position: { x: 160, y: 0 },
        color: '',
        publishedAt: new Date('2024-01-01'),
        isMiniNode: true,
        linkedBranchNode: subNode100.id,
      };

      const structuredGroups = [
        new Map([[MASTER_BRANCH_NAME, [miniNode94, miniNode100]]]),
        new Map([['release/9.4', [subNode94]]]),
        new Map([['release/10.0', [subNode100]]]),
      ];

      const skipNodes: SkipNode[] = [{
        id: 'skip-mini-sub-9.4-1-mini-sub-10.0-1',
        x: 110,
        y: 0,
        skippedCount: 1,
        skippedVersions: ['v9.5.0'],
        label: '1 skipped',
        sourceNodeId: miniNode94.id,
        targetNodeId: miniNode100.id,
      }];

      const links = service.createLinks(structuredGroups, skipNodes);

      const directLink = links.find(
        (link) => link.source === miniNode94.id && link.target === miniNode100.id,
      );

      expect(directLink).withContext('expected no direct link when a skip node covers the gap').toBeUndefined();
    });
  });

  describe('(private) isVersionGap', () => {
    it('should return false for consecutive minor versions', () => {
      const source = createMockNode('v1.1.0', undefined, 'v1.1.0');
      const target = createMockNode('v1.2.0', undefined, 'v1.2.0')

      expect((service as any).isVersionGap(source, target)).toBe(false);
    });

    it('should return true for consecutive major versions', () => {
      const source = createMockNode('v1.0.0', undefined, 'v1.0.0');
      const target = createMockNode('v2.0.0', undefined, 'v2.0.0')

      expect((service as any).isVersionGap(source, target)).toBe(true);
    });

    it('should return false for patch versions', () => {
      const source = createMockNode('v1.1.0', undefined, 'v1.1.0');
      const target = createMockNode('v1.1.1', undefined, 'v1.1.1');

      expect((service as any).isVersionGap(source, target)).toBe(false);
    });

    it('should return true for a minor version gap', () => {
      const source = createMockNode('v1.1.0', undefined, 'v1.1.0');
      const target = createMockNode('v1.3.0', undefined, 'v1.3.0');

      expect((service as any).isVersionGap(source, target)).toBe(true);
    });

    it('should return true for a major version gap', () => {
      const source = createMockNode('v1.0.0', undefined, 'v1.0.0');
      const target = createMockNode('v3.0.0', undefined, 'v3.0.0');

      expect((service as any).isVersionGap(source, target)).toBe(true);
    });

    it('should return true for a gap with patches involved', () => {
      const source = createMockNode('v1.1.5', undefined, 'v1.1.5');
      const target = createMockNode('v1.3.0', undefined, 'v1.3.0');

      expect((service as any).isVersionGap(source, target)).toBe(true);
    });
  });

  describe('Integration Tests - Skip Node and Link Creation', () => {
    it('should create complete skip node system with links for complex scenario', () => {
      const masterNodes = [
        createMockNode('v4.0.0', undefined, 'v4.0.0'),
        createMockNode('v7.0.0', undefined, 'v7.0.0'),
        createMockNode('v9.0.0', undefined, 'v9.0.0')
      ];
      masterNodes[0].position = { x: 450, y: 0 };
      masterNodes[1].position = { x: 900, y: 0 };
      masterNodes[2].position = { x: 1350, y: 0 };

      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, masterNodes]])];
      const allReleases: Release[] = [
        {
          id: 'v1.0.0',
          name: 'v1.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v2.0.0',
          name: 'v2.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v3.0.0',
          name: 'v3.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v5.0.0',
          name: 'v5.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v6.0.0',
          name: 'v6.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'v8.0.0',
          name: 'v8.0.0',
          branch: { name: MASTER_BRANCH_NAME },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];

      mockNodeService.timelineScale = { pixelsPerDay: 1 } as any;

      const skipNodes = service.createSkipNodes(structuredGroups, allReleases);
      const skipNodeLinks = service.createSkipNodeLinks(skipNodes, masterNodes);
      const allLinks = service.createLinks(structuredGroups, skipNodes);

      expect(skipNodes.length).toBe(3);
      expect(skipNodeLinks.length).toBe(5);

      const fadeInLink = allLinks.find(link => link.source.startsWith('start-node-'));

      expect(fadeInLink).toBeDefined();
      expect(fadeInLink?.target).toBe('skip-initial-v4.0.0');
    });
  });
});
