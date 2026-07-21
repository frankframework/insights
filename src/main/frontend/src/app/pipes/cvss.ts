// The source this logic is based on is: https://www.first.org/cvss/v3-1/specification-document#Scoring
// Here you can see all the metric equations and values that are being used in the original calculator which are now used inside our calculate functions.

export type CvssMetricKey = 'AV' | 'AC' | 'PR' | 'UI' | 'S' | 'C' | 'I' | 'A';

export interface CvssMetricOption {
  key: string;
  fullLabel: string;
  description: string;
}

export interface CvssMetricDefinition {
  key: CvssMetricKey;
  label: string;
  description: string;
  options: CvssMetricOption[];
}

export type CvssVector = Partial<Record<CvssMetricKey, string>>;

export type CvssSeverity = 'None' | 'Low' | 'Medium' | 'High' | 'Critical';

export interface CvssResult {
  score: number;
  severity: CvssSeverity;
}

const METRIC_ORDER: CvssMetricKey[] = ['AV', 'AC', 'PR', 'UI', 'S', 'C', 'I', 'A'];

export const CVSS_METRICS: CvssMetricDefinition[] = [
  {
    key: 'AV',
    label: 'Attack Vector',
    description: 'Reflects the context by which vulnerability exploitation is possible.',
    options: [
      {
        key: 'N',
        fullLabel: 'Network',
        description:
          'The vulnerable component is bound to the network stack and the attacker can reach it remotely, up to and including the entire Internet.',
      },
      {
        key: 'A',
        fullLabel: 'Adjacent',
        description:
          'The attack is limited to protocols on a logically adjacent topology, such as the same shared physical network, Bluetooth, or local IP subnet.',
      },
      {
        key: 'L',
        fullLabel: 'Local',
        description:
          'The attacker requires local access (keyboard, console) or relies on a separate user opening a malicious file or program.',
      },
      {
        key: 'P',
        fullLabel: 'Physical',
        description: 'The attack requires the attacker to physically touch or manipulate the vulnerable component.',
      },
    ],
  },
  {
    key: 'AC',
    label: 'Attack Complexity',
    description:
      'Describes the conditions beyond the attacker’s control that must exist in order to exploit the vulnerability.',
    options: [
      {
        key: 'L',
        fullLabel: 'Low',
        description:
          'No specialized conditions exist; an attacker can expect repeatable success against the vulnerable component.',
      },
      {
        key: 'H',
        fullLabel: 'High',
        description:
          'A successful attack depends on conditions beyond the attacker’s control, requiring significant preparation before exploitation.',
      },
    ],
  },
  {
    key: 'PR',
    label: 'Privileges Required',
    description:
      'Describes the level of privileges an attacker must possess before successfully exploiting the vulnerability.',
    options: [
      {
        key: 'N',
        fullLabel: 'None',
        description: 'The attacker is unauthorized prior to the attack and requires no access to settings or files.',
      },
      {
        key: 'L',
        fullLabel: 'Low',
        description:
          'The attacker requires privileges that provide basic user capabilities, typically only affecting settings and files owned by that user.',
      },
      {
        key: 'H',
        fullLabel: 'High',
        description:
          'The attacker requires privileges that provide significant, e.g. administrative, control over the vulnerable component.',
      },
    ],
  },
  {
    key: 'UI',
    label: 'User Interaction',
    description: 'Captures whether a human user, other than the attacker, must participate for the attack to succeed.',
    options: [
      {
        key: 'N',
        fullLabel: 'None',
        description: 'The vulnerable system can be exploited without any interaction from any user.',
      },
      {
        key: 'R',
        fullLabel: 'Required',
        description:
          'Successful exploitation requires a user to take some action before the vulnerability can be exploited.',
      },
    ],
  },
  {
    key: 'S',
    label: 'Scope',
    description:
      'Captures whether a vulnerability in one vulnerable component impacts resources beyond its own security scope.',
    options: [
      {
        key: 'U',
        fullLabel: 'Unchanged',
        description: 'An exploited vulnerability can only affect resources managed by the same security authority.',
      },
      {
        key: 'C',
        fullLabel: 'Changed',
        description:
          'An exploited vulnerability can affect resources beyond its own security scope, i.e. a different security authority.',
      },
    ],
  },
  {
    key: 'C',
    label: 'Confidentiality',
    description:
      'Measures the impact to the confidentiality of information resources managed by the affected component.',
    options: [
      {
        key: 'N',
        fullLabel: 'None',
        description: 'There is no loss of confidentiality within the impacted component.',
      },
      {
        key: 'L',
        fullLabel: 'Low',
        description:
          'Some loss of confidentiality occurred; the attacker does not control what information is obtained, or the loss is constrained.',
      },
      {
        key: 'H',
        fullLabel: 'High',
        description:
          'Total loss of confidentiality; all resources within the impacted component are disclosed to the attacker.',
      },
    ],
  },
  {
    key: 'I',
    label: 'Integrity',
    description:
      'Measures the impact to the trustworthiness and veracity of information managed by the affected component.',
    options: [
      { key: 'N', fullLabel: 'None', description: 'There is no loss of integrity within the impacted component.' },
      {
        key: 'L',
        fullLabel: 'Low',
        description:
          'Modification of data is possible, but the attacker does not control the consequence, or the amount of modification is limited.',
      },
      {
        key: 'H',
        fullLabel: 'High',
        description:
          'Total loss of integrity; the attacker can modify any or all files protected by the impacted component.',
      },
    ],
  },
  {
    key: 'A',
    label: 'Availability',
    description: 'Measures the impact to the availability of the affected component itself.',
    options: [
      { key: 'N', fullLabel: 'None', description: 'There is no impact to availability within the impacted component.' },
      {
        key: 'L',
        fullLabel: 'Low',
        description: 'Performance is reduced or there are interruptions in resource availability.',
      },
      {
        key: 'H',
        fullLabel: 'High',
        description:
          'Total loss of availability; the attacker can fully deny access to resources in the impacted component.',
      },
    ],
  },
];

const AV_WEIGHTS: Record<string, number> = { N: 0.85, A: 0.62, L: 0.55, P: 0.2 };
const AC_WEIGHTS: Record<string, number> = { L: 0.77, H: 0.44 };
const PR_WEIGHTS_UNCHANGED: Record<string, number> = { N: 0.85, L: 0.62, H: 0.27 };
const PR_WEIGHTS_CHANGED: Record<string, number> = { N: 0.85, L: 0.68, H: 0.5 };
const UI_WEIGHTS: Record<string, number> = { N: 0.85, R: 0.62 };
const CIA_WEIGHTS: Record<string, number> = { N: 0, L: 0.22, H: 0.56 };

export function isVectorComplete(vector: CvssVector): boolean {
  return METRIC_ORDER.every((key) => !!vector[key]);
}

export function vectorToString(vector: CvssVector): string {
  const parts = METRIC_ORDER.filter((key) => vector[key]).map((key) => `${key}:${vector[key]}`);
  return parts.length > 0 ? `CVSS:3.1/${parts.join('/')}` : '';
}

export function parseVectorString(input: string): CvssVector | null {
  const trimmed = input.trim();
  if (!trimmed) return null;

  const withoutPrefix = trimmed.replace(/^CVSS:3\.[01]\//i, '');
  const segments = withoutPrefix.split('/').filter(Boolean);
  const vector: CvssVector = {};

  for (const segment of segments) {
    const [rawKey, rawValue] = segment.split(':');
    if (!rawKey || !rawValue) continue;

    const metricKey = rawKey.toUpperCase() as CvssMetricKey;
    const definition = CVSS_METRICS.find((metric) => metric.key === metricKey);
    const validOption = definition?.options.find((option) => option.key === rawValue.toUpperCase());
    if (validOption) {
      vector[metricKey] = validOption.key;
    }
  }

  return isVectorComplete(vector) ? vector : null;
}

function roundUp(input: number): number {
  const intInput = Math.round(input * 100_000);
  if (intInput % 10_000 === 0) {
    return intInput / 100_000;
  }
  return (Math.floor(intInput / 10_000) + 1) / 10;
}

export function severityForScore(score: number): CvssSeverity {
  if (score <= 0) return 'None';
  if (score < 4) return 'Low';
  if (score < 7) return 'Medium';
  if (score < 9) return 'High';
  return 'Critical';
}

export function calculateCvssScore(vector: CvssVector): CvssResult | null {
  if (!isVectorComplete(vector)) return null;

  const scopeChanged = vector.S === 'C';
  const av = AV_WEIGHTS[vector.AV as string];
  const ac = AC_WEIGHTS[vector.AC as string];
  const pr = (scopeChanged ? PR_WEIGHTS_CHANGED : PR_WEIGHTS_UNCHANGED)[vector.PR as string];
  const ui = UI_WEIGHTS[vector.UI as string];
  const c = CIA_WEIGHTS[vector.C as string];
  const index = CIA_WEIGHTS[vector.I as string];
  const a = CIA_WEIGHTS[vector.A as string];

  const iscBase = 1 - (1 - c) * (1 - index) * (1 - a);
  const impact = scopeChanged ? 7.52 * (iscBase - 0.029) - 3.25 * Math.pow(iscBase - 0.02, 15) : 6.42 * iscBase;

  if (impact <= 0) {
    return { score: 0, severity: 'None' };
  }

  const exploitability = 8.22 * av * ac * pr * ui;

  const score = scopeChanged
    ? roundUp(Math.min(1.08 * (impact + exploitability), 10))
    : roundUp(Math.min(impact + exploitability, 10));

  return { score, severity: severityForScore(score) };
}
