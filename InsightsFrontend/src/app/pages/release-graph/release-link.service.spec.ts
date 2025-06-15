import { TestBed } from '@angular/core/testing';
import { ReleaseLinkService } from './release-link.service';
import { ReleaseNode } from './release-node.service';

const MASTER_BRANCH_NAME = 'master';

const createMockNode = (id: string, originalBranch?: string): ReleaseNode => ({
  id,
  label: id,
  branch: originalBranch ?? MASTER_BRANCH_NAME,
  originalBranch,
  position: { x: 0, y: 0 },
  color: '',
  publishedAt: new Date(),
});

describe('ReleaseLinkService', () => {
  let service: ReleaseLinkService;

  const masterNode1 = createMockNode('master-1');
  const masterNode2 = createMockNode('master-2');
  const anchorNode8 = createMockNode('anchor-8.0', 'release/8.0');
  const masterNode3 = createMockNode('master-3');

  const subNode8_1 = createMockNode('sub-8.0-1', 'release/8.0');
  const subNode8_2 = createMockNode('sub-8.0-2', 'release/8.0');

  const anchorNode9 = createMockNode('anchor-9.0', 'release/9.0');
  const subNode9_1 = createMockNode('sub-9.0-1', 'release/9.0');

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReleaseLinkService],
    });
    service = TestBed.inject(ReleaseLinkService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('createLinks() - Edge Cases', () => {
    it('should return an empty array if the input structuredGroups is empty', () => {
      const links = service.createLinks([]);
      expect(links).toEqual([]);
    });

    it('should only create intra-branch links for master if no sub-branches are provided', () => {
      const structuredGroups = [new Map([[MASTER_BRANCH_NAME, [masterNode1, masterNode2, masterNode3]]])];
      const links = service.createLinks(structuredGroups);

      expect(links.length).toBe(2);
      expect(links[0].source).toBe('master-1');
      expect(links[0].target).toBe('master-2');
      expect(links[1].source).toBe('master-2');
      expect(links[1].target).toBe('master-3');
    });

    it('should not crash and return an empty array if master branch is missing or empty', () => {
      // Scenario 1: No master branch map
      const noMasterGroups = [new Map([['release/9.0', [subNode9_1]]])];
      const links1 = service.createLinks(noMasterGroups);
      expect(links1).toEqual([]);

      // Scenario 2: Master branch exists but has no nodes
      const emptyMasterGroups = [new Map([[MASTER_BRANCH_NAME, []]])];
      const links2 = service.createLinks(emptyMasterGroups);
      expect(links2).toEqual([]);
    });

    it('should not create links for a sub-branch that has an empty node array', () => {
      const masterNodes = [masterNode1, masterNode2];
      const subGroups = [new Map([['empty-branch', []]])];
      const links = (service as any).createSubBranchLinks(subGroups, masterNodes);
      expect(links.length).toBe(0);
    });
  });

  describe('Core Linking Logic', () => {
    let masterNodes: ReleaseNode[];
    let structuredGroups: Map<string, ReleaseNode[]>[];

    beforeEach(() => {
      masterNodes = [masterNode1, anchorNode8, anchorNode9, masterNode3];
      structuredGroups = [
        new Map([[MASTER_BRANCH_NAME, masterNodes]]),
        new Map([['release/8.0', [subNode8_1, subNode8_2]]]),
        new Map([['release/9.0', [subNode9_1]]]),
      ];
    });

    it('should create all expected links for a full graph structure', () => {
      const links = service.createLinks(structuredGroups);
      expect(links.length).toBe(6);
    });

    it('should create a correct anchor link from a master node to a sub-branch node', () => {
      const links = service.createLinks(structuredGroups);
      const anchorLink = links.find(link => link.source === 'anchor-8.0' && link.target.startsWith('sub-'));

      expect(anchorLink).toBeDefined();
      expect(anchorLink?.target).toBe('sub-8.0-1');
    });

    it('should create correct intra-branch links for a sub-branch', () => {
      const links = service.createLinks(structuredGroups);
      const subBranchLink = links.find(link => link.source === 'sub-8.0-1');

      expect(subBranchLink).toBeDefined();
      expect(subBranchLink?.target).toBe('sub-8.0-2');
    });

    it('should NOT create an anchor link if its anchor node is missing from the master list', () => {
      const incompleteMasterNodes = [masterNode1, anchorNode8, masterNode3];
      const groups = [
        new Map([[MASTER_BRANCH_NAME, incompleteMasterNodes]]),
        new Map([['release/9.0', [subNode9_1]]]),
      ];
      const links = service.createLinks(groups);

      const missingAnchorLink = links.find(link => link.target === 'sub-9.0-1');
      expect(missingAnchorLink).toBeUndefined();
    });
  });

  describe('createIntraBranchLinks()', () => {
    it('should return an empty array for a branch with 0 nodes', () => {
      // Accessing private method for dedicated unit test
      const links = (service as any).createIntraBranchLinks([]);
      expect(links).toEqual([]);
    });

    it('should return an empty array for a branch with only 1 node', () => {
      const links = (service as any).createIntraBranchLinks([masterNode1]);
      expect(links).toEqual([]);
    });

    it('should create N-1 links for a branch with N nodes', () => {
      const nodes = [masterNode1, masterNode2, masterNode3];
      const links = (service as any).createIntraBranchLinks(nodes);
      expect(links.length).toBe(2);
      expect(links[0].id).toBe('master-1-master-2');
      expect(links[1].id).toBe('master-2-master-3');
    });
  });
});
