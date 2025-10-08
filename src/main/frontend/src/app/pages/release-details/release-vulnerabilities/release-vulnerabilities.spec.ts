import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReleaseVulnerabilities } from './release-vulnerabilities';
import { Vulnerability, VulnerabilitySeverities } from '../../../services/vulnerability.service';

describe('ReleaseVulnerabilities', () => {
  let component: ReleaseVulnerabilities;
  let fixture: ComponentFixture<ReleaseVulnerabilities>;

  const mockVulnerabilities: Vulnerability[] = [
    {
      cveId: 'CVE-2024-0001',
      severity: VulnerabilitySeverities.CRITICAL,
      cvssScore: 9.8,
      description: 'Critical vulnerability with a very long description that should trigger the see more button because it exceeds six lines of text when rendered in the UI component with normal font size and line height settings.',
      cwes: ['CWE-79', 'CWE-89']
    },
    {
      cveId: 'CVE-2024-0002',
      severity: VulnerabilitySeverities.HIGH,
      cvssScore: 7.5,
      description: 'High severity vulnerability',
      cwes: ['CWE-22']
    },
    {
      cveId: 'CVE-2024-0003',
      severity: VulnerabilitySeverities.MEDIUM,
      cvssScore: 5.0,
      description: 'Medium severity vulnerability',
      cwes: []
    },
    {
      cveId: 'CVE-2024-0004',
      severity: VulnerabilitySeverities.LOW,
      cvssScore: 2.1,
      description: 'Low severity vulnerability',
      cwes: ['CWE-200']
    },
    {
      cveId: 'CVE-2024-0005',
      severity: VulnerabilitySeverities.CRITICAL,
      cvssScore: 10.0,
      description: 'Another critical vulnerability',
      cwes: ['CWE-78']
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReleaseVulnerabilities]
    }).compileComponents();

    fixture = TestBed.createComponent(ReleaseVulnerabilities);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with empty vulnerabilities', () => {
      expect(component.vulnerabilities).toEqual([]);
      expect(component.sortedVulnerabilities).toEqual([]);
      expect(component.selectedVulnerability).toBeNull();
    });

    it('should initialize with isDescriptionExpanded as false', () => {
      expect(component.isDescriptionExpanded).toBe(false);
    });

    it('should initialize with showSeeMoreButton as false', () => {
      expect(component.showSeeMoreButton).toBe(false);
    });
  });

  describe('Vulnerability Sorting', () => {
    it('should sort vulnerabilities by severity (CRITICAL first)', () => {
      component.vulnerabilities = mockVulnerabilities;
      component.ngOnChanges();

      expect(component.sortedVulnerabilities[0].severity).toBe(VulnerabilitySeverities.CRITICAL);
      expect(component.sortedVulnerabilities[component.sortedVulnerabilities.length - 1].severity).toBe(VulnerabilitySeverities.LOW);
    });

    it('should sort vulnerabilities within same severity by CVSS score (highest first)', () => {
      component.vulnerabilities = mockVulnerabilities;
      component.ngOnChanges();

      const criticalVulns = component.sortedVulnerabilities.filter(
        v => v.severity === VulnerabilitySeverities.CRITICAL
      );
      expect(criticalVulns[0].cvssScore).toBe(10.0);
      expect(criticalVulns[1].cvssScore).toBe(9.8);
    });

    it('should handle empty vulnerabilities array', () => {
      component.vulnerabilities = [];
      component.ngOnChanges();

      expect(component.sortedVulnerabilities).toEqual([]);
      expect(component.selectedVulnerability).toBeNull();
    });

    it('should handle single vulnerability', () => {
      component.vulnerabilities = [mockVulnerabilities[0]];
      component.ngOnChanges();

      expect(component.sortedVulnerabilities.length).toBe(1);
      expect(component.selectedVulnerability).toBe(mockVulnerabilities[0]);
    });

    it('should auto-select first vulnerability after sorting', () => {
      component.vulnerabilities = mockVulnerabilities;
      component.ngOnChanges();

      expect(component.selectedVulnerability).toBe(component.sortedVulnerabilities[0]);
    });
  });

  describe('Vulnerability Selection', () => {
    beforeEach(() => {
      component.vulnerabilities = mockVulnerabilities;
      component.ngOnChanges();
      fixture.detectChanges();
    });

    it('should select vulnerability on click', () => {
      const targetVuln = component.sortedVulnerabilities[2];
      component.selectVulnerability(targetVuln);

      expect(component.selectedVulnerability).toBe(targetVuln);
    });

    it('should reset isDescriptionExpanded when selecting new vulnerability', () => {
      component.isDescriptionExpanded = true;
      component.selectVulnerability(component.sortedVulnerabilities[1]);

      expect(component.isDescriptionExpanded).toBe(false);
    });

    it('should call window.scrollTo when selecting vulnerability', (done) => {
      const mockElement = document.createElement('div');
      mockElement.className = 'cwe-details';
      document.body.appendChild(mockElement);

      spyOn(window, 'scrollTo');

      let rafCallback: FrameRequestCallback | null = null;
      spyOn(window, 'requestAnimationFrame').and.callFake((callback: FrameRequestCallback) => {
        rafCallback = callback;
        return 0;
      });

      component.selectVulnerability(component.sortedVulnerabilities[1]);

      setTimeout(() => {
        if (rafCallback) {
          rafCallback(performance.now() + 1000);
        }

        expect(window.scrollTo).toHaveBeenCalled();
        document.body.removeChild(mockElement);
        done();
      }, 20);
    });
  });

  describe('Description Expansion', () => {
    beforeEach(() => {
      component.vulnerabilities = mockVulnerabilities;
      component.ngOnChanges();
      fixture.detectChanges();
    });

    it('should toggle description expansion', () => {
      expect(component.isDescriptionExpanded).toBe(false);
      component.toggleDescription();
      expect(component.isDescriptionExpanded).toBe(true);
      component.toggleDescription();
      expect(component.isDescriptionExpanded).toBe(false);
    });
  });

  describe('Severity Styling', () => {
    it('should return correct CSS class for CRITICAL severity', () => {
      expect(component.getSeverityClass(VulnerabilitySeverities.CRITICAL)).toBe('severity-critical');
    });

    it('should return correct CSS class for HIGH severity', () => {
      expect(component.getSeverityClass(VulnerabilitySeverities.HIGH)).toBe('severity-high');
    });

    it('should return correct CSS class for MEDIUM severity', () => {
      expect(component.getSeverityClass(VulnerabilitySeverities.MEDIUM)).toBe('severity-medium');
    });

    it('should return correct CSS class for LOW severity', () => {
      expect(component.getSeverityClass(VulnerabilitySeverities.LOW)).toBe('severity-low');
    });

    it('should return correct CSS class for NONE severity', () => {
      expect(component.getSeverityClass(VulnerabilitySeverities.NONE)).toBe('severity-none');
    });

    it('should return correct CSS class for UNKNOWN severity', () => {
      expect(component.getSeverityClass(VulnerabilitySeverities.UNKNOWN)).toBe('severity-unknown');
    });
  });

  describe('CWE URL Generation', () => {
    it('should generate correct CWE URL from CWE-79', () => {
      expect(component.getCweUrl('CWE-79')).toBe('https://cwe.mitre.org/data/definitions/79.html');
    });

    it('should generate correct CWE URL from CWE-89', () => {
      expect(component.getCweUrl('CWE-89')).toBe('https://cwe.mitre.org/data/definitions/89.html');
    });

    it('should handle invalid CWE format gracefully', () => {
      expect(component.getCweUrl('INVALID')).toBe('#');
    });

    it('should handle empty CWE string', () => {
      expect(component.getCweUrl('')).toBe('#');
    });

    it('should extract number from CWE with extra text', () => {
      expect(component.getCweUrl('CWE-200: Information Exposure')).toBe('https://cwe.mitre.org/data/definitions/200.html');
    });
  });

  describe('CVSS Score Formatting', () => {
    it('should format integer score without decimal', () => {
      expect(component.formatCvssScore(9)).toBe('9');
    });

    it('should format decimal score with one decimal place', () => {
      expect(component.formatCvssScore(9.5)).toBe('9.5');
    });

    it('should format score ending in .0 without decimal', () => {
      expect(component.formatCvssScore(10.0)).toBe('10');
    });

    it('should round score to one decimal place', () => {
      expect(component.formatCvssScore(7.89)).toBe('7.9');
    });

    it('should handle zero score', () => {
      expect(component.formatCvssScore(0)).toBe('0');
    });

    it('should handle maximum score', () => {
      expect(component.formatCvssScore(10)).toBe('10');
    });
  });

  describe('Edge Cases', () => {
    it('should handle vulnerability with no CWEs', () => {
      const vulnNoCwe: Vulnerability = {
        cveId: 'CVE-2024-9999',
        severity: VulnerabilitySeverities.HIGH,
        cvssScore: 8.0,
        description: 'No CWEs',
        cwes: []
      };
      component.vulnerabilities = [vulnNoCwe];
      component.ngOnChanges();

      expect(component.sortedVulnerabilities[0].cwes).toEqual([]);
    });

    it('should handle vulnerability with empty description', () => {
      const vulnNoDesc: Vulnerability = {
        cveId: 'CVE-2024-8888',
        severity: VulnerabilitySeverities.MEDIUM,
        cvssScore: 5.5,
        description: '',
        cwes: ['CWE-79']
      };
      component.vulnerabilities = [vulnNoDesc];
      component.ngOnChanges();

      expect(component.sortedVulnerabilities[0].description).toBe('');
    });

    it('should handle vulnerability with very long CVE ID', () => {
      const vulnLongId: Vulnerability = {
        cveId: 'CVE-2024-0001-VERY-LONG-IDENTIFIER-WITH-EXTRA-TEXT',
        severity: VulnerabilitySeverities.LOW,
        cvssScore: 3.0,
        description: 'Test',
        cwes: []
      };
      component.vulnerabilities = [vulnLongId];
      component.ngOnChanges();

      expect(component.sortedVulnerabilities[0].cveId).toContain('VERY-LONG');
    });

    it('should handle negative CVSS score', () => {
      expect(component.formatCvssScore(-1)).toBe('-1');
    });

    it('should handle CVSS score above 10', () => {
      expect(component.formatCvssScore(11.5)).toBe('11.5');
    });

    it('should handle multiple vulnerabilities with same severity and score', () => {
      component.vulnerabilities = [
        {
          cveId: 'CVE-2024-1111',
          severity: VulnerabilitySeverities.HIGH,
          cvssScore: 7.5,
          description: 'Duplicate 1',
          cwes: []
        },
        {
          cveId: 'CVE-2024-2222',
          severity: VulnerabilitySeverities.HIGH,
          cvssScore: 7.5,
          description: 'Duplicate 2',
          cwes: []
        }
      ];
      component.ngOnChanges();

      expect(component.sortedVulnerabilities.length).toBe(2);
      expect(component.sortedVulnerabilities[0].cvssScore).toBe(7.5);
      expect(component.sortedVulnerabilities[1].cvssScore).toBe(7.5);
    });
  });

  describe('Component State Management', () => {
    it('should maintain selected vulnerability after re-sorting', () => {
      component.vulnerabilities = mockVulnerabilities;
      component.ngOnChanges();
      const selected = component.sortedVulnerabilities[2];
      component.selectVulnerability(selected);

      component.ngOnChanges();

      expect(component.selectedVulnerability).toBeDefined();
    });

    it('should reset expansion state on ngOnChanges', () => {
      component.isDescriptionExpanded = true;
      component.ngOnChanges();

      expect(component.isDescriptionExpanded).toBe(false);
    });
  });
});
