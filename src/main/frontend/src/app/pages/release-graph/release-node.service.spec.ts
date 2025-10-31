import { TestBed } from '@angular/core/testing';
import { ReleaseNode, ReleaseNodeService, SupportColors, TimelineScale } from './release-node.service';
import { Branch, Release } from '../../services/release.service';

const MASTER_BRANCH_NAME = 'master';

const createMockData = (): Release[] => {
  const branches: Record<string, Branch> = {
    master: { id: 'b-master', name: MASTER_BRANCH_NAME },
    b90: { id: 'b-90', name: 'release/9.0' },
    b84: { id: 'b-84', name: 'release/8.4' },
    b72: { id: 'b-72', name: 'release/7.2' },
    b_single: { id: 'b-single', name: 'release/6.5' },
  };

  return [
    {
      id: 'master-1',
      name: 'v8.0',
      publishedAt: new Date('2023-12-20T10:00:00Z'),
      branch: branches['master'],
      tagName: '',
    },
    {
      id: 'master-2',
      name: 'v7.0',
      publishedAt: new Date('2022-12-20T10:00:00Z'),
      branch: branches['master'],
      tagName: '',
    },

    {
      id: '9.0-anchor',
      name: 'v9.0.0',
      publishedAt: new Date('2025-01-10T10:00:00Z'),
      branch: branches['b90'],
      tagName: '',
    },
    {
      id: '9.0-node-1',
      name: 'v9.0.1',
      publishedAt: new Date('2025-02-10T10:00:00Z'),
      branch: branches['b90'],
      tagName: '',
    },
    {
      id: '9.0-nightly',
      name: 'v9.0.2 (nightly)',
      publishedAt: new Date('2025-03-10T10:00:00Z'),
      branch: branches['b90'],
      tagName: '',
    },
    {
      id: '8.4-anchor',
      name: 'v8.4.0',
      publishedAt: new Date('2025-04-15T10:00:00Z'),
      branch: branches['b84'],
      tagName: '',
    },
    {
      id: '8.4-node-1',
      name: 'v8.4.1',
      publishedAt: new Date('2025-05-15T10:00:00Z'),
      branch: branches['b84'],
      tagName: '',
    },
    {
      id: '7.2-anchor',
      name: 'v7.2.0',
      publishedAt: new Date('2022-06-01T10:00:00Z'),
      branch: branches['b72'],
      tagName: '',
    },
    {
      id: '7.2-node-1',
      name: 'v7.2.1',
      publishedAt: new Date('2022-07-01T10:00:00Z'),
      branch: branches['b72'],
      tagName: '',
    },

    // Branch met 1 release
    {
      id: '6.5-anchor',
      name: 'v6.5.0',
      publishedAt: new Date('2025-01-01T10:00:00Z'),
      branch: branches['b_single'],
      tagName: '',
    },
  ];
};

describe('ReleaseNodeService', () => {
  let service: ReleaseNodeService;
  let mockReleases: Release[];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReleaseNodeService],
    });
    service = TestBed.inject(ReleaseNodeService);
    mockReleases = createMockData() as any;

    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2025-06-15T12:00:00Z'));
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('structureReleaseData', () => {
    it('should move the first release of a sub-branch to the master branch as an anchor', () => {
      const structuredData = service.structureReleaseData(mockReleases);
      const masterNodes = structuredData[0].get(MASTER_BRANCH_NAME)!;

      const anchorNode = masterNodes.find((node) => node.id === '8.4-anchor');

      expect(anchorNode).toBeDefined();
      expect(anchorNode?.originalBranch).toBe('release/8.4');
    });

    it('should keep the master branch chronologically sorted after adding anchors', () => {
      const structuredData = service.structureReleaseData(mockReleases);
      const masterNodes = structuredData[0].get(MASTER_BRANCH_NAME)!;

      for (let index = 0; index < masterNodes.length - 1; index++) {
        const timeA = masterNodes[index].publishedAt.getTime();
        const timeB = masterNodes[index + 1].publishedAt.getTime();

        expect(timeA).toBeLessThanOrEqual(timeB);
      }
    });
  });

  describe('calculateReleaseCoordinates', () => {
    let structuredData: Map<string, ReleaseNode[]>[];

    beforeEach(() => {
      structuredData = service.structureReleaseData(mockReleases);
    });

    it('should sort sub-branches by version number (descending), not by date', () => {
      const positionedMap = service.calculateReleaseCoordinates(structuredData);
      const branchOrder = [...positionedMap.keys()].filter((b) => b !== MASTER_BRANCH_NAME);

      expect(branchOrder).toEqual(['release/9.0', 'release/8.4']);
    });

    it('should position master nodes on Y=0 axis', () => {
      const positionedMap = service.calculateReleaseCoordinates(structuredData);
      const masterNodes = positionedMap.get(MASTER_BRANCH_NAME)!;
      for (const node of masterNodes) {
        expect(node.position.y).toBe(0);
      }
    });

    it('should position sub-branch nodes on a positive Y axis', () => {
      const positionedMap = service.calculateReleaseCoordinates(structuredData);
      const subBranchNodes = positionedMap.get('release/9.0')!;
      for (const node of subBranchNodes) {
        expect(node.position.y).toBeGreaterThan(0);
      }
    });

    it('should create a timeline scale with quarters', () => {
      service.calculateReleaseCoordinates(structuredData);

      expect(service.timelineScale).toBeDefined();
      expect(service.timelineScale).not.toBeNull();
      expect(service.timelineScale!.quarters).toBeDefined();
      expect(service.timelineScale!.quarters.length).toBeGreaterThan(0);
    });

    it('should calculate X positions based on timeline dates', () => {
      const positionedMap = service.calculateReleaseCoordinates(structuredData);
      const masterNodes = positionedMap.get(MASTER_BRANCH_NAME)!;

      for (const node of masterNodes) {
        expect(node.position.x).toBeGreaterThanOrEqual(0);
        expect(Number.isFinite(node.position.x)).toBe(true);
      }

      for (let index = 1; index < masterNodes.length; index++) {
        expect(masterNodes[index].position.x).toBeGreaterThanOrEqual(masterNodes[index - 1].position.x);
      }
    });
  });

  describe('assignReleaseColors and determineColor', () => {
    const NOW = new Date('2025-06-15T12:00:00Z');
    const TWO_WEEKS_AGO = new Date(NOW.getTime() - 14 * 24 * 60 * 60 * 1000);
    const TWO_MONTHS_AGO = new Date(NOW.getTime() - 60 * 24 * 60 * 60 * 1000);
    const FOUR_MONTHS_AGO = new Date(NOW.getTime() - 120 * 24 * 60 * 60 * 1000);
    const FIVE_MONTHS_AGO = new Date(NOW.getTime() - 150 * 24 * 60 * 60 * 1000);
    const THREE_YEARS_AGO = new Date(NOW.getTime() - 3 * 365 * 24 * 60 * 60 * 1000);

    it('should assign FULL support color for a recent major release (v9.0.0)', () => {
      const node = { label: 'v9.0.0', publishedAt: FIVE_MONTHS_AGO } as any;
      const color = (service as any).determineColor(node, false, false);

      expect(color).toBe(SupportColors.FULL);
    });

    it('should assign SECURITY support color for an older minor release (v8.4.0)', () => {
      const node = { label: 'v8.4.0', publishedAt: FOUR_MONTHS_AGO } as any;
      const color = (service as any).determineColor(node, false, false);

      expect(color).toBe(SupportColors.SECURITY);
    });

    it('should assign NONE support color for an unsupported release (v7.2.0)', () => {
      const node = { label: 'v7.2.0', publishedAt: THREE_YEARS_AGO } as any;
      const color = (service as any).determineColor(node, false, false);

      expect(color).toBe(SupportColors.NONE);
    });

    it('should assign darkblue for a nightly release', () => {
      const node = { label: 'v9.0.2 (nightly)' } as any;
      const color = (service as any).determineColor(node, false, false);

      expect(color).toBe('darkblue');
    });

    describe('latest patch version logic', () => {
      it('should assign NONE color to older patch versions regardless of support dates', () => {
        const node = { label: 'v8.4.1', publishedAt: TWO_WEEKS_AGO } as any;
        const isPatchVersion = true;
        const isLatestPatch = false;
        const color = (service as any).determineColor(node, isPatchVersion, isLatestPatch);

        expect(color).toBe(SupportColors.NONE);
      });

      it('should assign normal support colors to the latest patch version', () => {
        const node = { label: 'v8.4.2', publishedAt: TWO_WEEKS_AGO } as any;
        const isPatchVersion = true;
        const isLatestPatch = true;
        const color = (service as any).determineColor(node, isPatchVersion, isLatestPatch);

        expect(color).toBe(SupportColors.FULL);
      });

      it('should not affect major versions (v9.0.0) - they get their own support colors', () => {
        const node = { label: 'v9.0.0', publishedAt: FIVE_MONTHS_AGO } as any;
        const isPatchVersion = false;
        const isLatestPatch = false;
        const color = (service as any).determineColor(node, isPatchVersion, isLatestPatch);

        expect(color).toBe(SupportColors.FULL);
      });

      it('should not affect minor versions (v8.4.0) - they get their own support colors', () => {
        const node = { label: 'v8.4.0', publishedAt: FOUR_MONTHS_AGO } as any;
        const isPatchVersion = false;
        const isLatestPatch = false;
        const color = (service as any).determineColor(node, isPatchVersion, isLatestPatch);

        expect(color).toBe(SupportColors.SECURITY);
      });
    });

    describe('assignReleaseColors - integration with latest patch logic', () => {
      it('should identify and color only the latest patch in a series correctly', () => {
        const nodes: ReleaseNode[] = [
          { id: '1', label: 'v8.4.1', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '2', label: 'v8.4.2', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '3', label: 'v8.4.3', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
        ];

        const releaseGroups = new Map([['master', nodes]]);
        service.assignReleaseColors(releaseGroups);

        expect(nodes[0].color).toBe(SupportColors.NONE);
        expect(nodes[1].color).toBe(SupportColors.NONE);
        expect(nodes[2].color).toBe(SupportColors.FULL);
      });

      it('should handle multiple version series independently', () => {
        const nodes: ReleaseNode[] = [
          { id: '1', label: 'v7.8.1', publishedAt: THREE_YEARS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '2', label: 'v7.8.2', publishedAt: THREE_YEARS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '3', label: 'v8.4.1', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '4', label: 'v8.4.2', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
        ];

        const releaseGroups = new Map([['master', nodes]]);
        service.assignReleaseColors(releaseGroups);

        expect(nodes[0].color).toBe(SupportColors.NONE);
        expect(nodes[1].color).toBe(SupportColors.NONE);
        expect(nodes[2].color).toBe(SupportColors.NONE);
        expect(nodes[3].color).toBe(SupportColors.FULL);
      });

      it('should not affect major and minor version colors', () => {
        const nodes: ReleaseNode[] = [
          { id: '1', label: 'v8.0.0', publishedAt: FIVE_MONTHS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '2', label: 'v8.4.0', publishedAt: TWO_MONTHS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '3', label: 'v8.4.1', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
        ];

        const releaseGroups = new Map([['master', nodes]]);
        service.assignReleaseColors(releaseGroups);

        expect(nodes[0].color).toBe(SupportColors.FULL);
        expect(nodes[1].color).toBe(SupportColors.FULL);
        expect(nodes[2].color).toBe(SupportColors.FULL);
      });
    });
  });

  describe('getVersionFromBranchName', () => {
    it('should parse standard release branches', () => {
      const version = (service as any).getVersionFromBranchName('release/9.0');

      expect(version).toEqual({ major: 9, minor: 0 });
    });

    it('should parse branches with different formats', () => {
      const version = (service as any).getVersionFromBranchName('my-branch-8.1-release');

      expect(version).toEqual({ major: 8, minor: 1 });
    });

    it('should return null for branches without a version number', () => {
      const version = (service as any).getVersionFromBranchName('feature/new-button');

      expect(version).toBeNull();
    });
  });

  describe('Timeline Scale Calculation', () => {
    // eslint-disable-next-line unicorn/consistent-function-scoping
    const createNode = (date: string): ReleaseNode => ({
      id: date,
      label: 'v1',
      position: { x: 0, y: 0 },
      color: 'red',
      branch: 'master',
      publishedAt: new Date(date),
    });

    it('should return a default scale for empty nodes', () => {
      const scale = (service as any).calculateTimelineScale([]);

      expect(scale.totalDays).toBe(0);
      expect(scale.pixelsPerDay).toBe(1);
    });

    it('should calculate scale based on node dates', () => {
      const nodes = [createNode('2024-02-15T00:00:00Z'), createNode('2024-08-01T00:00:00Z')];
      const scale = (service as any).calculateTimelineScale(nodes);

      expect(scale.latestReleaseDate).toEqual(new Date('2024-08-01T00:00:00Z'));
      expect(scale.startDate).toEqual(new Date(2023, 9, 1));
      expect(scale.endDate).toEqual(new Date(2024, 11, 30));
      expect(scale.pixelsPerDay).toBeCloseTo(2.222);
    });

    it('should generate correct quarter markers', () => {
      const startDate = new Date('2023-10-01T00:00:00Z');
      const endDate = new Date('2024-05-01T00:00:00Z');
      const pixelsPerDay = 2;
      const markers = (service as any).generateQuarterMarkers(startDate, endDate, pixelsPerDay);

      // Q4 2023, Q1 2024, Q2 2024
      expect(markers.length).toBe(3);
      expect(markers[0].label).toBe('Q4 2023');
      expect(markers[1].label).toBe('Q1 2024');
      expect(markers[2].label).toBe('Q2 2024');
    });

    it('should calculate X position from date', () => {
      const scale: TimelineScale = {
        startDate: new Date('2024-01-01T00:00:00Z'),
        endDate: new Date('2024-12-31T00:00:00Z'),
        pixelsPerDay: 2,
        totalDays: 365,
        quarters: [],
        latestReleaseDate: new Date(),
      };

      const date1 = new Date('2024-01-01T00:00:00Z');
      const date2 = new Date('2024-01-11T00:00:00Z');

      expect((service as any).calculateXPositionFromDate(date1, scale)).toBe(0);
      expect((service as any).calculateXPositionFromDate(date2, scale)).toBe(20);
    });
  });

  describe('createClusters', () => {
    beforeEach(() => {
      const structuredData = service.structureReleaseData(mockReleases);
      service.calculateReleaseCoordinates(structuredData);
    });

    it('should return original nodes if timeline scale is not set', () => {
      service.timelineScale = null;
      const nodes: ReleaseNode[] = [
        { id: '1', label: 'v1.0', position: { x: 100, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
        { id: '2', label: 'v1.1', position: { x: 120, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
      ];

      const result = service.createClusters(nodes);

      expect(result).toEqual(nodes);
    });

    it('should return original nodes if array is empty', () => {
      const result = service.createClusters([]);

      expect(result).toEqual([]);
    });

    it('should create clusters for closely positioned nodes', () => {
      const now = new Date();
      const nodes: ReleaseNode[] = [
        { id: '1', label: 'v9.0.1', position: { x: 1000, y: 0 }, branch: 'master', color: 'green', publishedAt: now },
        { id: '2', label: 'v9.0.2', position: { x: 1050, y: 0 }, branch: 'master', color: 'green', publishedAt: now },
        { id: '3', label: 'v9.0.3', position: { x: 1090, y: 0 }, branch: 'master', color: 'green', publishedAt: now },
      ];

      if (service.timelineScale) {
        service.timelineScale.latestReleaseDate = now;
      }

      const result = service.createClusters(nodes);

      const clusterNode = result.find(n => n.isCluster);
      if (clusterNode) {
        expect(clusterNode.clusteredNodes).toBeDefined();
        expect(clusterNode.clusteredNodes!.length).toBeGreaterThan(1);
      }
    });

    it('should not cluster nodes that are far apart', () => {
      const oldDate = new Date('2023-01-01');
      const nodes: ReleaseNode[] = [
        { id: '1', label: 'v7.0', position: { x: 100, y: 0 }, branch: 'master', color: 'green', publishedAt: oldDate },
        { id: '2', label: 'v8.0', position: { x: 1000, y: 0 }, branch: 'master', color: 'green', publishedAt: oldDate },
      ];

      const result = service.createClusters(nodes);

      expect(result.length).toBe(2);
      expect(result.every(n => !n.isCluster)).toBe(true);
    });
  });

  describe('expandCluster', () => {
    it('should expand cluster into individual nodes with spacing', () => {
      const clusteredNodes: ReleaseNode[] = [
        { id: '1', label: 'v9.0.1', position: { x: 0, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
        { id: '2', label: 'v9.0.2', position: { x: 0, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
        { id: '3', label: 'v9.0.3', position: { x: 0, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
      ];
      const clusterNode: ReleaseNode = {
        id: 'cluster-1',
        label: '3',
        position: { x: 500, y: 0 },
        branch: 'master',
        color: '#dee2e6',
        publishedAt: new Date(),
        isCluster: true,
        clusteredNodes,
      };

      const result = service.expandCluster(clusterNode);

      expect(result.length).toBe(3);

      const avgX = result.reduce((sum, n) => sum + n.position.x, 0) / result.length;

      expect(Math.abs(avgX - 500)).toBeLessThan(1); // Should be very close to 500
    });

    it('should give extra spacing to nightly releases', () => {
      const clusteredNodes: ReleaseNode[] = [
        { id: '1', label: 'v9.0.1', position: { x: 0, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
        { id: '2', label: 'v9.0.2-nightly', position: { x: 0, y: 0 }, branch: 'master', color: 'blue', publishedAt: new Date() },
        { id: '3', label: 'v9.0.3', position: { x: 0, y: 0 }, branch: 'master', color: 'green', publishedAt: new Date() },
      ];
      const clusterNode: ReleaseNode = {
        id: 'cluster-1',
        label: '3',
        position: { x: 500, y: 0 },
        branch: 'master',
        color: '#dee2e6',
        publishedAt: new Date(),
        isCluster: true,
        clusteredNodes,
      };

      const result = service.expandCluster(clusterNode);

      expect(result.length).toBe(3);
      const spacing1 = result[1].position.x - result[0].position.x;
      const spacing2 = result[2].position.x - result[1].position.x;

      const BASE_SPACING = 60;
      const NIGHTLY_EXTRA_SPACING = 60;

      expect(spacing1).toBe(BASE_SPACING + NIGHTLY_EXTRA_SPACING);
      expect(spacing2).toBe(BASE_SPACING + NIGHTLY_EXTRA_SPACING)
    });

    it('should return original node if not a cluster', () => {
      const regularNode: ReleaseNode = {
        id: '1',
        label: 'v9.0.1',
        position: { x: 500, y: 0 },
        branch: 'master',
        color: 'green',
        publishedAt: new Date(),
      };

      const result = service.expandCluster(regularNode);

      expect(result).toEqual([regularNode]);
    });

    it('should handle empty clustered nodes', () => {
      const clusterNode: ReleaseNode = {
        id: 'cluster-1',
        label: '0',
        position: { x: 500, y: 0 },
        branch: 'master',
        color: '#dee2e6',
        publishedAt: new Date(),
        isCluster: true,
        clusteredNodes: [],
      };

      const result = service.expandCluster(clusterNode);

      expect(result).toEqual([clusterNode]);
    });
  });
});
