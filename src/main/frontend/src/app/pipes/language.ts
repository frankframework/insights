export interface LanguageInfo {
  id: string;
  label: string;
  domain: 'frontend' | 'backend' | 'unknown';
}

const LANGUAGE_BY_PURL_TYPE: Record<string, { label: string; domain: 'frontend' | 'backend' }> = {
  maven: { label: 'Java (Maven)', domain: 'backend' },
  npm: { label: 'TypeScript (npm)', domain: 'frontend' },
};

export function extractPurlType(purl: string | null | undefined): string | null {
  if (!purl) return null;
  const match = /^pkg:([^/]+)\//.exec(purl);
  return match ? match[1].toLowerCase() : null;
}

export function languageFromPurl(purl: string | null | undefined): LanguageInfo | null {
  const type = extractPurlType(purl);
  if (!type) return null;

  const known = LANGUAGE_BY_PURL_TYPE[type];
  return { id: type, label: known?.label ?? type, domain: known?.domain ?? 'unknown' };
}
