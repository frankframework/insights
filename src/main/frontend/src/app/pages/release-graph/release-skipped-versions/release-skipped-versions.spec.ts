import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReleaseSkippedVersions } from './release-skipped-versions';
import { SkipNode } from '../release-link.service';
import { Release } from '../../../services/release.service';
import { parseVersionRanges, VersionRange } from '../../../pipes/release-range';

const rangesOf = (specification: string): VersionRange[] => parseVersionRanges(specification).ranges;

const buildReleaseTree = (component: ReleaseSkippedVersions, versions: string[]): void => {
  const skipNode: SkipNode = {
    id: 'skip-1',
    x: 100,
    y: 0,
    skippedCount: versions.length,
    skippedVersions: versions,
    label: `${versions.length} skipped`,
  };

  component.skipNode = skipNode;
  component.releases = versions.map((version, index) => ({
    id: `r${index}`,
    name: version,
    branch: { name: 'master' },
    tagName: '',
    publishedAt: new Date(),
    lastScanned: new Date(),
  }));

  component.ngOnChanges({
    skipNode: { currentValue: skipNode, previousValue: null, firstChange: true, isFirstChange: () => true },
    releases: { currentValue: component.releases, previousValue: [], firstChange: true, isFirstChange: () => true },
  });
};

describe('ReleaseSkippedVersions', () => {
  let component: ReleaseSkippedVersions;
  let fixture: ComponentFixture<ReleaseSkippedVersions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseSkippedVersions],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReleaseSkippedVersions);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('structureSkippedReleases', () => {
    it('should create release tree from skipped versions', () => {
      const skipNode: SkipNode = {
        id: 'skip-1',
        x: 100,
        y: 0,
        skippedCount: 3,
        skippedVersions: ['v7.0.0', 'v7.1.0', 'v7.2.0'],
        label: '3 skipped',
      };

      const releases: Release[] = [
        {
          id: 'r1',
          name: 'v7.0.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date,
          lastScanned: new Date(),
        },
        {
          id: 'r2',
          name: 'v7.1.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'r3',
          name: 'v7.2.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date,
        },
      ];

      component.skipNode = skipNode;
      component.releases = releases;
      component.ngOnChanges({
        skipNode: { currentValue: skipNode, previousValue: null, firstChange: true, isFirstChange: () => true },
        releases: { currentValue: releases, previousValue: [], firstChange: true, isFirstChange: () => true },
      });

      expect(component.releaseTree.length).toBe(3);
      expect(component.releaseTree[0].version).toBe('v7.0.0');
      expect(component.releaseTree[1].version).toBe('v7.1.0');
      expect(component.releaseTree[2].version).toBe('v7.2.0');
    });

    it('should group patch releases under their parent minor version', () => {
      const skipNode: SkipNode = {
        id: 'skip-1',
        x: 100,
        y: 0,
        skippedCount: 3,
        skippedVersions: ['v7.1.0', 'v7.1.1', 'v7.1.2'],
        label: '3 skipped',
      };

      const releases: Release[] = [
        {
          id: 'r1',
          name: 'v7.1.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'r2',
          name: 'v7.1.1',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'r3',
          name: 'v7.1.2',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];

      component.skipNode = skipNode;
      component.releases = releases;
      component.ngOnChanges({
        skipNode: { currentValue: skipNode, previousValue: null, firstChange: true, isFirstChange: () => true },
        releases: { currentValue: releases, previousValue: [], firstChange: true, isFirstChange: () => true },
      });

      expect(component.releaseTree.length).toBe(1);
      expect(component.releaseTree[0].version).toBe('v7.1.0');
      expect(component.releaseTree[0].patches.length).toBe(2);
      expect(component.releaseTree[0].patches[0].name).toBe('v7.1.1');
      expect(component.releaseTree[0].patches[1].name).toBe('v7.1.2');
    });

    it('should sort releases by version number', () => {
      const skipNode: SkipNode = {
        id: 'skip-1',
        x: 100,
        y: 0,
        skippedCount: 3,
        skippedVersions: ['v7.2.0', 'v7.0.0', 'v7.1.0'],
        label: '3 skipped',
      };

      const releases: Release[] = [
        {
          id: 'r1',
          name: 'v7.2.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        }, {
          id: 'r2',
          name: 'v7.0.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'r3',
          name: 'v7.1.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
  ];

      component.skipNode = skipNode;
      component.releases = releases;
      component.ngOnChanges({
        skipNode: { currentValue: skipNode, previousValue: null, firstChange: true, isFirstChange: () => true },
        releases: { currentValue: releases, previousValue: [], firstChange: true, isFirstChange: () => true },
      });

      expect(component.releaseTree[0].version).toBe('v7.0.0');
      expect(component.releaseTree[1].version).toBe('v7.1.0');
      expect(component.releaseTree[2].version).toBe('v7.2.0');
    });

    it('should group patch releases under the major (x.0.0) parent, not create a duplicate minor entry', () => {
      const skipNode: SkipNode = {
        id: 'skip-1',
        x: 100,
        y: 0,
        skippedCount: 1,
        skippedVersions: ['v9.0.0', 'v9.0.1', 'v9.0.2'],
        label: '1 skipped',
      };

      const releases: Release[] = [
        {
          id: 'r1',
          name: 'v9.0.0',
          branch: { name: 'release/9.0' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'r2',
          name: 'v9.0.1',
          branch: { name: 'release/9.0' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
        {
          id: 'r3',
          name: 'v9.0.2',
          branch: { name: 'release/9.0' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];

      component.skipNode = skipNode;
      component.releases = releases;
      component.ngOnChanges({
        skipNode: { currentValue: skipNode, previousValue: null, firstChange: true, isFirstChange: () => true },
        releases: { currentValue: releases, previousValue: [], firstChange: true, isFirstChange: () => true },
      });

      expect(component.releaseTree.length).toBe(1);
      expect(component.releaseTree[0].version).toBe('v9.0.0');
      expect(component.releaseTree[0].type).toBe('major');
      expect(component.releaseTree[0].patches.length).toBe(2);
    });

    it('should handle releases without v prefix', () => {
      const skipNode: SkipNode = {
        id: 'skip-1',
        x: 100,
        y: 0,
        skippedCount: 1,
        skippedVersions: ['v7.0.0'],
        label: '1 skipped',
      };

      const releases: Release[] = [
        {
          id: 'r1',
          name: 'v7.0.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },      ];

      component.skipNode = skipNode;
      component.releases = releases;
      component.ngOnChanges({
        skipNode: { currentValue: skipNode, previousValue: null, firstChange: true, isFirstChange: () => true },
        releases: { currentValue: releases, previousValue: [], firstChange: true, isFirstChange: () => true },
      });

      expect(component.releaseTree.length).toBe(1);
      expect(component.releaseTree[0].version).toBe('v7.0.0');
    });

    it('should emit versionClicked event when version is clicked', () => {
      spyOn(component.versionClicked, 'emit');

      component.onVersionClick('v7.0.0');

      expect(component.versionClicked.emit).toHaveBeenCalledWith('v7.0.0');
    });

    it('should emit closed event when modal is closed', () => {
      spyOn(component.closed, 'emit');

      component.closeModal();

      expect(component.closed.emit).toHaveBeenCalledWith();
    });

    it('should not create release tree if skipNode is null', () => {
      component.skipNode = null;
      component.releases = [
        {
          id: 'r1',
          name: 'v7.0.0',
          branch: { name: 'master' },
          tagName: '',
          publishedAt: new Date(),
          lastScanned: new Date(),
        },
      ];
      component.ngOnChanges({
        releases: { currentValue: component.releases, previousValue: [], firstChange: true, isFirstChange: () => true },
      });

      expect(component.releaseTree.length).toBe(0);
    });

    it('should not create release tree if releases array is empty', () => {
      const skipNode: SkipNode = {
        id: 'skip-1',
        x: 100,
        y: 0,
        skippedCount: 1,
        skippedVersions: ['v7.0.0'],
        label: '1 skipped',
      };

      component.skipNode = skipNode;
      component.releases = [];
      component.ngOnChanges({
        skipNode: { currentValue: skipNode, previousValue: null, firstChange: true, isFirstChange: () => true },
      });

      expect(component.releaseTree.length).toBe(0);
    });
  });

  describe('including releases', () => {
    it('should label the release lines it can include', () => {
      buildReleaseTree(component, ['v7.1.0', 'v7.2.0', 'v7.3.0']);

      expect(component.includeLabel).toBe('[7.1,7.3]');
    });

    it('should label a single release line', () => {
      buildReleaseTree(component, ['v7.1.0', 'v7.1.1']);

      expect(component.includeLabel).toBe('[7.1]');
    });

    it('should label release lines with a gap between them apart', () => {
      buildReleaseTree(component, ['v7.1.0', 'v7.3.0']);

      expect(component.includeLabel).toBe('[7.1],[7.3]');
    });

    it('should emit a release line per skipped root release', () => {
      spyOn(component.rangeRequested, 'emit');
      buildReleaseTree(component, ['v7.1.0', 'v7.3.0']);

      component.includeReleases();

      expect(component.rangeRequested.emit).toHaveBeenCalledWith(rangesOf('[7.1],[7.3]'));
    });

    it('should emit every patch of a release line, not only the listed ones', () => {
      spyOn(component.rangeRequested, 'emit');
      buildReleaseTree(component, ['v7.1.0', 'v7.1.2']);

      component.includeReleases();

      expect(component.rangeRequested.emit).toHaveBeenCalledWith(rangesOf('[7.1]'));
    });

    it('should not emit when there is nothing to include', () => {
      spyOn(component.rangeRequested, 'emit');

      component.includeReleases();

      expect(component.rangeRequested.emit).not.toHaveBeenCalled();
    });

    it('should hold a single version as pending instead of emitting it', () => {
      spyOn(component.rangeRequested, 'emit');

      component.includeVersion('v9.3.2');

      expect(component.rangeRequested.emit).not.toHaveBeenCalled();
      expect(component.isVersionPending('v9.3.2')).toBeTrue();
    });

    it('should emit the pending versions when they are applied', () => {
      spyOn(component.rangeRequested, 'emit');

      component.includeVersion('v9.3.2');
      component.applyPendingIncludes();

      expect(component.rangeRequested.emit).toHaveBeenCalledWith(rangesOf('[9.3.2]'));
    });

    it('should drop a version again when it is removed from pending', () => {
      component.includeVersion('v9.3.2');
      component.includeVersion('v9.3.4');
      component.removeFromPending('v9.3.2');

      expect(component.isVersionPending('v9.3.2')).toBeFalse();
      expect(component.isVersionPending('v9.3.4')).toBeTrue();
    });

    it('should not emit for a version without a version number', () => {
      spyOn(component.rangeRequested, 'emit');

      component.includeVersion('master');

      expect(component.rangeRequested.emit).not.toHaveBeenCalled();
      expect(component.hasPending).toBeFalse();
    });

    it('should report already included when the range covers every release line', () => {
      buildReleaseTree(component, ['v7.1.0', 'v7.2.0']);
      component.releaseRanges = rangesOf('[7.0,8.0]');

      expect(component.isAlreadyIncluded).toBeTrue();
    });

    it('should not report already included when a release line falls outside the range', () => {
      buildReleaseTree(component, ['v7.1.0', 'v9.0.0']);
      component.releaseRanges = rangesOf('[7.0,8.0]');

      expect(component.isAlreadyIncluded).toBeFalse();
    });

    it('should not report already included when only part of a release line is in range', () => {
      buildReleaseTree(component, ['v7.1.0', 'v7.1.5']);
      component.releaseRanges = rangesOf('[7.1.0,7.1.2]');

      expect(component.isAlreadyIncluded).toBeFalse();
    });

    it('should report a single version as included when the range covers it', () => {
      component.releaseRanges = rangesOf('[9.3]');

      expect(component.isVersionIncluded('v9.3.2')).toBeTrue();
      expect(component.isVersionIncluded('v9.4.0')).toBeFalse();
    });
  });
});
