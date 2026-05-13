/**
 * Determines whether a release branch is still within its security support window.
 *
 * Mirrors the support-duration logic in ReleaseNodeService.getSupportEndDates:
 *   - Major branch (minor === 0, e.g. "8.0" / "release/8.0"): 12 months security support (1 year)
 *   - Minor branch (minor > 0, e.g. "7.8" / "release/7.8"):    6 months security support (half year)
 *
 * @param branchName          e.g. "release/8.0", "release/7.8", "8.0" or "7.8"
 * @param earliestPublishedAt publishedAt of the first known release on that branch
 */
export function isBranchMaintained(branchName: string, earliestPublishedAt: string | Date): boolean {
  // Strip optional "release/" (or any path) prefix before version parsing
  const normalized = branchName.replace(/^.*\//, '').toLowerCase();

  // master and nightly branches are always considered maintained
  if (normalized === 'master' || normalized.endsWith('nightly')) return true;

  const match = normalized.match(/^v?(\d+)\.(\d+)/);
  if (!match) return true;

  const minor = Number.parseInt(match[2], 10);
  // Mirrors ReleaseNodeService.getSupportEndDates
  const securitySupportMonths = minor === 0 ? 12 : 6;

  const basePublishedDate = new Date(earliestPublishedAt);
  const securitySupportEnd = new Date(basePublishedDate);
  securitySupportEnd.setMonth(basePublishedDate.getMonth() + securitySupportMonths);

  return new Date() <= securitySupportEnd;
}
