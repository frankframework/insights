import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReleaseNode, ReleaseNodeService, SupportColors, TimelineScale } from './release-node.service';
import { Branch, Release } from '../../services/release.service';

const MASTER_BRANCH_NAME = 'master';

const createMockData = (): Release[] => {
  const branches: Record<string, Branch> = {
    master: { id: 'b-master', name: MASTER_BRANCH_NAME },
    b90: { id: 'b-90', name: 'release/9.0' },
    b84: { id: 'b-84', name: 'release/8.4' },
    b72: { id: 'b-72', name: 'release/7.2' },
  };

  return [
    {
      id: 'master-1',
      name: 'v8.0',
      publishedAt: new Date('2023-12-20T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['master'],
      tagName: 'v8.0',
    },
    {
      id: 'master-2',
      name: 'v7.0',
      publishedAt: new Date('2022-12-20T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['master'],
      tagName: 'v7.0',
    },
    {
      id: 'master-nightly',
      name: 'v9.4.0-20251108.042330 (nightly)',
      publishedAt: new Date('2025-06-10T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['master'],
      tagName: 'release/9.4-nightly',
    },

    {
      id: '9.0-anchor',
      name: 'v9.0.0',
      publishedAt: new Date('2025-01-10T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['b90'],
      tagName: 'release/v9.0.0',
    },
    {
      id: '9.0-node-1',
      name: 'v9.0.1',
      publishedAt: new Date('2025-02-10T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['b90'],
      tagName: 'release/v9.0.1',
    },
    {
      id: '9.0-nightly',
      name: 'v9.0.2 (nightly)',
      publishedAt: new Date('2025-03-10T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['b90'],
      tagName: 'v9.0.2-nightly',
    },
    {
      id: '8.4-anchor',
      name: 'v8.4.0',
      publishedAt: new Date('2025-04-15T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['b84'],
      tagName: 'release/v8.4.0',
    },
    {
      id: '8.4-node-1',
      name: 'v8.4.1',
      publishedAt: new Date('2025-05-15T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['b84'],
      tagName: 'release/v8.4.1',
    },
    {
      id: '7.2-anchor',
      name: 'v7.2.0',
      publishedAt: new Date('2022-06-01T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['b72'],
      tagName: 'release/v7.2.0',
    },
    {
      id: '7.2-node-1',
      name: 'v7.2.1',
      publishedAt: new Date('2022-07-01T10:00:00Z'),
      lastScanned: new Date(),
      branch: branches['b72'],
      tagName: 'release/v7.2.1',
    },
  ];
};

describe('ReleaseNodeService', () => {
  let service: ReleaseNodeService;
  let mockReleases: Release[];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReleaseNodeService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
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

      const anchorNode = masterNodes.find((node) => node.originalBranch === 'release/8.4');

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

    it('should remove older nightly releases for a branch, keeping only the latest one', () => {
      const branchName = 'feature/test-branch';
      const branches: Record<string, Branch> = {
        testNightlyBranch: { id: 'b-test', name: branchName },
      };

      const nightlyReleases: Release[] = [
        { id: 'R1-ANCHOR', name: 'v1.0.0', publishedAt: new Date('2024-10-01T10:00:00Z'), lastScanned: new Date(), branch: branches['testNightlyBranch'], tagName: 'v1.0.0' },
        { id: 'N1-OLD', name: 'v1.0.1 (nightly)', publishedAt: new Date('2024-11-01T10:00:00Z'), lastScanned: new Date(), branch: branches['testNightlyBranch'], tagName: 'v1.0.1-nightly' },
        { id: 'N2-OLDER', name: 'v1.0.2 (nightly)', publishedAt: new Date('2024-11-15T10:00:00Z'), lastScanned: new Date(), branch: branches['testNightlyBranch'], tagName: 'v1.0.2-nightly' },
        { id: 'N3-LATEST', name: 'v1.0.3 (nightly)', publishedAt: new Date('2024-11-20T10:00:00Z'), lastScanned: new Date(), branch: branches['testNightlyBranch'], tagName: 'v1.0.3-nightly' },
      ];

      const allReleases = [...mockReleases, ...nightlyReleases];

      const structuredData = service.structureReleaseData(allReleases);

      const subBranchMap = structuredData.find(m => m.has(branchName));
      const subBranchNodes = subBranchMap?.get(branchName) ?? [];

      expect(subBranchNodes.length).toBe(2);
      expect(subBranchNodes[0].id).toBe('R1-ANCHOR');
      expect(subBranchNodes[1].id).toBe('N3-LATEST');
      expect(subBranchNodes.some(n => n.id === 'N1-OLD')).toBeFalse();
      expect(subBranchNodes.some(n => n.id === 'N2-OLDER')).toBeFalse();
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
      const subBranchNodes = positionedMap.get('release/9.0');

      expect(subBranchNodes).toBeDefined();
      expect(subBranchNodes!.length).toBeGreaterThan(0);

      for (const node of subBranchNodes!) {
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

  describe('transformNodeLabel', () => {
    it('should strip "release/" prefix and format nightly releases as vX.Y.Z-nightly', () => {
      const mockRelease: Release = {
        id: '1', name: 'v1.2.3 (nightly)', publishedAt: new Date(), lastScanned: new Date(), branch: { id: 'b', name: 'dev' },
        tagName: 'release/v1.2.3-nightly'
      };
      const label = (service as any).transformNodeLabel(mockRelease);

      expect(label).toBe('v1.2.3-nightly');
    });

    it('should format nightly releases with version from release.name', () => {
      const mockRelease: Release = {
        id: '1', name: 'v1.2.3-20251108.042330 (nightly)', publishedAt: new Date(), lastScanned: new Date(), branch: { id: 'b', name: 'dev' },
        tagName: 'v1.2.3-nightly-2025'
      };
      const label = (service as any).transformNodeLabel(mockRelease);

      expect(label).toBe('v1.2.3-nightly');
    });

    it('should format master nightly releases as vX.Y.Z-nightly', () => {
      const mockRelease: Release = {
        id: '1',
        name: 'v9.4.0-20251108.042330 (nightly)',
        publishedAt: new Date(),
        lastScanned: new Date(),
        branch: { id: 'b', name: 'master' },
        tagName: 'release/9.4-nightly'
      };
      const label = (service as any).transformNodeLabel(mockRelease);

      expect(label).toBe('v9.4.0-nightly');
    });

    it('should handle nightly when release.name lacks "v" prefix', () => {
      const mockRelease: Release = {
        id: '1',
        name: '9.4.0-20251108.042330 (nightly)',
        publishedAt: new Date(),
        lastScanned: new Date(),
        branch: { id: 'b', name: 'master' },
        tagName: 'release/9.4-nightly'
      };
      const label = (service as any).transformNodeLabel(mockRelease);

      expect(label).toBe('v9.4.0-nightly');
    });

    it('should handle releases without nightly tags and strip prefix', () => {
      const mockRelease: Release = {
        id: '1', name: 'v10.0.0', publishedAt: new Date(), lastScanned: new Date(), branch: { id: 'b', name: '10.0' },
        tagName: 'release/v10.0.0'
      };
      const label = (service as any).transformNodeLabel(mockRelease);

      expect(label).toBe('v10.0.0');
    });
  });

  describe('assignReleaseColors', () => {
    const NOW = new Date('2025-06-15T12:00:00Z');
    const ONE_MONTH_AGO = new Date(NOW.getTime() - 30 * 24 * 60 * 60 * 1000);
    const THIRTEEN_MONTHS_AGO = new Date(NOW.getTime() - 395 * 24 * 60 * 60 * 1000);

    it('should assign CUTTING_EDGE color to master nightly', () => {
      const masterNightly: ReleaseNode = {
        id: 'master-nightly',
        label: 'v9.4.0-nightly',
        publishedAt: NOW,
        color: '',
        position: { x: 0, y: 0 },
        branch: MASTER_BRANCH_NAME,
      };

      const releaseGroups = new Map([[MASTER_BRANCH_NAME, [masterNightly]]]);
      service.assignReleaseColors(releaseGroups);

      expect(masterNightly.color).toBe(SupportColors.CUTTING_EDGE);
    });

    it('should assign LTS color to latest major release and its nightly', () => {
      const majorRelease: ReleaseNode = {
        id: 'major-1',
        label: 'v9.0.0',
        publishedAt: ONE_MONTH_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/9.0',
      };

      const majorNightly: ReleaseNode = {
        id: 'major-nightly',
        label: 'v9.0.1-nightly',
        publishedAt: NOW,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/9.0',
      };

      const releaseGroups = new Map([['release/9.0', [majorRelease, majorNightly]]]);
      service.assignReleaseColors(releaseGroups);

      expect(majorRelease.color).toBe(SupportColors.LTS);
      expect(majorNightly.color).toBe(SupportColors.LTS);
    });

    it('should assign LATEST_STABLE to latest minor release when it is not LTS', () => {
      const olderMajor: ReleaseNode = {
        id: 'major-old',
        label: 'v8.0.0',
        publishedAt: THIRTEEN_MONTHS_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: MASTER_BRANCH_NAME,
      };

      const latestMinor: ReleaseNode = {
        id: 'minor-latest',
        label: 'v8.4.0',
        publishedAt: ONE_MONTH_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/8.4',
      };

      const latestMinorNightly: ReleaseNode = {
        id: 'minor-nightly',
        label: 'v8.4.1-nightly',
        publishedAt: NOW,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/8.4',
      };

      const releaseGroups = new Map([
        [MASTER_BRANCH_NAME, [olderMajor]],
        ['release/8.4', [latestMinor, latestMinorNightly]],
      ]);
      service.assignReleaseColors(releaseGroups);

      expect(latestMinor.color).toBe(SupportColors.LATEST_STABLE);
      expect(latestMinorNightly.color).toBe(SupportColors.LATEST_STABLE);
    });

    it('should assign HISTORICAL color to all nodes initially', () => {
      const node1: ReleaseNode = {
        id: '1',
        label: 'v1.0.0',
        publishedAt: THIRTEEN_MONTHS_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: MASTER_BRANCH_NAME,
      };

      const node2: ReleaseNode = {
        id: '2',
        label: 'v2.0.0',
        publishedAt: THIRTEEN_MONTHS_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: MASTER_BRANCH_NAME,
      };

      const releaseGroups = new Map([[MASTER_BRANCH_NAME, [node1, node2]]]);
      service.assignReleaseColors(releaseGroups);

      expect(node1.color).toBe(SupportColors.HISTORICAL);
      expect(node2.color).toBe(SupportColors.HISTORICAL);
    });

    it('should not assign colors to mini nodes', () => {
      const regularNode: ReleaseNode = {
        id: 'regular',
        label: 'v9.0.0',
        publishedAt: ONE_MONTH_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/9.0',
      };

      const miniNode: ReleaseNode = {
        id: 'mini',
        label: '',
        publishedAt: ONE_MONTH_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: MASTER_BRANCH_NAME,
        isMiniNode: true,
      };

      const releaseGroups = new Map([['release/9.0', [regularNode, miniNode]]]);
      service.assignReleaseColors(releaseGroups);

      expect(regularNode.color).not.toBe('');
      expect(miniNode.color).toBe('');
    });

    it('should handle branches with only nightly releases', () => {
      const nightlyOnly: ReleaseNode = {
        id: 'nightly',
        label: 'v8.0.1-nightly',
        publishedAt: NOW,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/8.0',
      };

      const releaseGroups = new Map([['release/8.0', [nightlyOnly]]]);
      service.assignReleaseColors(releaseGroups);

      expect(nightlyOnly.color).toBeDefined();
    });

    it('should correctly identify and color the latest LTS even with multiple major versions', () => {
      const olderMajor: ReleaseNode = {
        id: 'old-major',
        label: 'v8.0.0',
        publishedAt: THIRTEEN_MONTHS_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/8.0',
      };

      const latestMajor: ReleaseNode = {
        id: 'latest-major',
        label: 'v9.0.0',
        publishedAt: ONE_MONTH_AGO,
        color: '',
        position: { x: 0, y: 0 },
        branch: 'release/9.0',
      };

      const releaseGroups = new Map([
        ['release/8.0', [olderMajor]],
        ['release/9.0', [latestMajor]],
      ]);
      service.assignReleaseColors(releaseGroups);

      expect(latestMajor.color).toBe(SupportColors.LTS);
      expect(olderMajor.color).toBe(SupportColors.HISTORICAL);
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

  describe('applyMinimumSpacing', () => {
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

      const result = service.applyMinimumSpacing(nodes);

      expect(result).toEqual(nodes);
    });

    it('should return original nodes if array is empty', () => {
      const result = service.applyMinimumSpacing([]);

      expect(result).toEqual([]);
    });

    it('should apply minimum spacing to closely positioned nodes in last year', () => {
      const now = new Date();
      const nodes: ReleaseNode[] = [
        { id: '1', label: 'v9.0.1', position: { x: 1000, y: 0 }, branch: 'master', color: 'green', publishedAt: now },
        { id: '2', label: 'v9.0.2', position: { x: 1010, y: 0 }, branch: 'master', color: 'green', publishedAt: now },
        { id: '3', label: 'v9.0.3', position: { x: 1020, y: 0 }, branch: 'master', color: 'green', publishedAt: now },
      ];

      if (service.timelineScale) {
        service.timelineScale.latestReleaseDate = now;
      }

      const result = service.applyMinimumSpacing(nodes);

      expect(result.length).toBe(3);
      const spacing1 = result[1].position.x - result[0].position.x;
      const spacing2 = result[2].position.x - result[1].position.x;

      expect(spacing1).toBeGreaterThanOrEqual(65);
      expect(spacing2).toBeGreaterThanOrEqual(65);
    });

    it('should not modify nodes that are far apart', () => {
      const oldDate = new Date('2023-01-01');
      const nodes: ReleaseNode[] = [
        { id: '1', label: 'v7.0', position: { x: 100, y: 0 }, branch: 'master', color: 'green', publishedAt: oldDate },
        { id: '2', label: 'v8.0', position: { x: 1000, y: 0 }, branch: 'master', color: 'green', publishedAt: oldDate },
      ];

      const result = service.applyMinimumSpacing(nodes);

      expect(result.length).toBe(2);
      expect(result[0].position.x).toBe(100);
      expect(result[1].position.x).toBe(1000);
    });
  });
});
