/**
 * Determines whether a release branch is still within its security support window.
 */
export function isBranchMaintained(branchName: string, earliestPublishedAt: string | Date): boolean {
  const normalized = branchName.replace(/^.*\//, '').toLowerCase();

  if (normalized === 'master' || normalized.endsWith('nightly')) return true;

  const match = normalized.match(/^v?(\d+)\.(\d+)/);
  if (!match) return true;

  const minor = Number.parseInt(match[2], 10);
  const securitySupportMonths = minor === 0 ? 12 : 6;

  const basePublishedDate = new Date(earliestPublishedAt);
  const securitySupportEnd = new Date(basePublishedDate);
  securitySupportEnd.setMonth(basePublishedDate.getMonth() + securitySupportMonths);

  return new Date() <= securitySupportEnd;
}
