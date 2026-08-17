export enum ViewMode {
  QUARTERLY = 'quarterly',
  MONTHLY = 'monthly',
}

export interface IssueStateStyle {
  [key: string]: string;
  'background-color': string;
  color: string;
  'border-color': string;
}

export const ISSUE_STATE_STYLES: Record<string, IssueStateStyle> = {
  Todo: {
    'background-color': '#f0fdf4',
    color: '#166534',
    'border-color': '#86efac',
  },
  'On hold': {
    'background-color': '#fee2e2',
    color: '#991b1b',
    'border-color': '#fca5a5',
  },
  'In Progress': {
    'background-color': '#dbeafe',
    color: '#1e3a8a',
    'border-color': '#93c5fd',
  },
  Review: {
    'background-color': '#fefce8',
    color: '#a16207',
    'border-color': '#fde047',
  },
  Done: {
    'background-color': '#f3e8ff',
    color: '#581c87',
    'border-color': '#d8b4fe',
  },
};

export const CLOSED_STYLE: IssueStateStyle = {
  'background-color': '#f3e8ff',
  color: '#581c87',
  'border-color': '#d8b4fe',
};

export const OPEN_STYLE: IssueStateStyle = {
  'background-color': '#f0fdf4',
  color: '#166534',
  'border-color': '#86efac',
};
