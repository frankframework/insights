import { TestBed } from '@angular/core/testing';
import { ReleaseNode, ReleaseNodeService, SupportColors } from './release-node.service';
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

    // Branch 8.4 releases
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

    // Branch 7.2 releases
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
  });

  describe('assignReleaseColors and determineColor', () => {
    const NOW = new Date('2025-06-15T12:00:00Z');
    // For patch/minor: 3 months full support, 6 months security support
    // For major: 6 months full support, 12 months security support
    const TWO_WEEKS_AGO = new Date(NOW.getTime() - 14 * 24 * 60 * 60 * 1000);
    const ONE_MONTH_AGO = new Date(NOW.getTime() - 30 * 24 * 60 * 60 * 1000);
    const TWO_MONTHS_AGO = new Date(NOW.getTime() - 60 * 24 * 60 * 60 * 1000);
    const FOUR_MONTHS_AGO = new Date(NOW.getTime() - 120 * 24 * 60 * 60 * 1000); // Within minor security (6m), past full (3m)
    const FIVE_MONTHS_AGO = new Date(NOW.getTime() - 150 * 24 * 60 * 60 * 1000); // Within major full (6m)
    const EIGHT_MONTHS_AGO = new Date(NOW.getTime() - 240 * 24 * 60 * 60 * 1000); // Within major security (12m), past minor security (6m)
    const THREE_YEARS_AGO = new Date(NOW.getTime() - 3 * 365 * 24 * 60 * 60 * 1000);

    beforeAll(() => {
      jasmine.clock().install();
      jasmine.clock().mockDate(NOW);
    });

    afterAll(() => {
      jasmine.clock().uninstall();
    });

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
        const coloredNodes = service.assignReleaseColors(releaseGroups);

        expect(coloredNodes[0].color).toBe(SupportColors.NONE); // v8.4.1 - older patch
        expect(coloredNodes[1].color).toBe(SupportColors.NONE); // v8.4.2 - older patch
        expect(coloredNodes[2].color).toBe(SupportColors.FULL); // v8.4.3 - latest patch
      });

      it('should handle multiple version series independently', () => {
        const nodes: ReleaseNode[] = [
          { id: '1', label: 'v7.8.1', publishedAt: THREE_YEARS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '2', label: 'v7.8.2', publishedAt: THREE_YEARS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '3', label: 'v8.4.1', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '4', label: 'v8.4.2', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
        ];

        const releaseGroups = new Map([['master', nodes]]);
        const coloredNodes = service.assignReleaseColors(releaseGroups);

        expect(coloredNodes[0].color).toBe(SupportColors.NONE); // v7.8.1 - older patch
        expect(coloredNodes[1].color).toBe(SupportColors.NONE); // v7.8.2 - latest but old (unsupported)
        expect(coloredNodes[2].color).toBe(SupportColors.NONE); // v8.4.1 - older patch
        expect(coloredNodes[3].color).toBe(SupportColors.FULL); // v8.4.2 - latest patch and supported
      });

      it('should not affect major and minor version colors', () => {
        const nodes: ReleaseNode[] = [
          { id: '1', label: 'v8.0.0', publishedAt: FIVE_MONTHS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '2', label: 'v8.4.0', publishedAt: TWO_MONTHS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
          { id: '3', label: 'v8.4.1', publishedAt: TWO_WEEKS_AGO, color: '', position: { x: 0, y: 0 }, branch: 'master' },
        ];

        const releaseGroups = new Map([['master', nodes]]);
        const coloredNodes = service.assignReleaseColors(releaseGroups);

        expect(coloredNodes[0].color).toBe(SupportColors.FULL); // v8.0.0 - major, gets own color
        expect(coloredNodes[1].color).toBe(SupportColors.FULL); // v8.4.0 - minor, gets own color (within 3 months)
        expect(coloredNodes[2].color).toBe(SupportColors.FULL); // v8.4.1 - latest patch
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
});
