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
      publishedAt: new Date('2021-01-01T10:00:00Z'),
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

    it('should correctly handle a branch with only one release by moving it to master', () => {
      const structuredData = service.structureReleaseData(mockReleases);
      const masterNodes = structuredData[0].get(MASTER_BRANCH_NAME)!;

      const anchorNode = masterNodes.find((node) => node.id === '6.5-anchor');

      expect(anchorNode).toBeDefined();
      expect(anchorNode?.originalBranch).toBe('release/6.5');

      const singleReleaseBranchMap = structuredData.find((map) => map.has('release/6.5'));

      expect(singleReleaseBranchMap).toBeUndefined();
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
    beforeAll(() => {
      jasmine.clock().install();
      jasmine.clock().mockDate(new Date('2025-06-15T12:00:00Z'));
    });

    afterAll(() => {
      jasmine.clock().uninstall();
    });

    it('should assign FULL support color for a recent major release (v9.0.0)', () => {
      const node = { label: 'v9.0.0', publishedAt: new Date('2025-01-10T10:00:00Z') } as any;
      const color = (service as any).determineColor(node);

      expect(color).toBe(SupportColors.FULL);
    });

    it('should assign SECURITY support color for an older major release (v8.4.0)', () => {
      const node = { label: 'v8.4.0', publishedAt: new Date('2024-01-15T10:00:00Z') } as any;
      const color = (service as any).determineColor(node);

      expect(color).toBe(SupportColors.SECURITY);
    });

    it('should assign NONE support color for an unsupported release (v7.2.0)', () => {
      const node = { label: 'v7.2.0', publishedAt: new Date('2022-06-01T10:00:00Z') } as any;
      const color = (service as any).determineColor(node);

      expect(color).toBe(SupportColors.NONE);
    });

    it('should assign darkblue for a nightly release', () => {
      const node = { label: 'v9.0.2 (nightly)' } as any;
      const color = (service as any).determineColor(node);

      expect(color).toBe('darkblue');
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
