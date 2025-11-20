import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReleaseSkippedVersions } from './release-skipped-versions';
import { SkipNode } from '../release-link.service';

interface Release {
  id: string;
  name: string;
  branch: { name: string };
}

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
        { id: 'r1', name: 'v7.0.0', branch: { name: 'master' } },
        { id: 'r2', name: 'v7.1.0', branch: { name: 'master' } },
        { id: 'r3', name: 'v7.2.0', branch: { name: 'master' } },
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
        { id: 'r1', name: 'v7.1.0', branch: { name: 'master' } },
        { id: 'r2', name: 'v7.1.1', branch: { name: 'master' } },
        { id: 'r3', name: 'v7.1.2', branch: { name: 'master' } },
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
        { id: 'r1', name: 'v7.2.0', branch: { name: 'master' } },
        { id: 'r2', name: 'v7.0.0', branch: { name: 'master' } },
        { id: 'r3', name: 'v7.1.0', branch: { name: 'master' } },
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
        { id: 'r1', name: '7.0.0', branch: { name: 'master' } },
      ];

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

      expect(component.closed.emit).toHaveBeenCalled();
    });

    it('should not create release tree if skipNode is null', () => {
      component.skipNode = null;
      component.releases = [
        { id: 'r1', name: 'v7.0.0', branch: { name: 'master' } },
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
});
