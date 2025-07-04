import { Injectable } from '@angular/core';
import { AppService, GitHubState } from '../app.service';
import { Milestone } from './milestone.service';
import { Label } from './label.service';
import { Observable } from 'rxjs';

export interface Issue {
  id: string;
  number: number;
  title: string;
  state: GitHubState;
  closedAt?: Date;
  url: string;
  businessValue?: string;
  milestone?: Milestone;
  issueType?: IssueType;
  issuePriority?: IssuePriority;
  points?: number;
  labels?: Label[];
  subIssues?: Issue[];
}

export interface IssueType {
  id: string;
  name: string;
  description: string;
  color: string;
}

export interface IssuePriority {
  id: string;
  name: string;
  color: string;
  description: string;
}

@Injectable({
  providedIn: 'root',
})
export class IssueService {
  constructor(private appService: AppService) {}

  public getIssuesByReleaseId(releaseId: string): Observable<Issue[]> {
    return this.appService.get<Issue[]>(this.appService.createAPIUrl(`issues/release/${releaseId}`));
  }

  public getIssuesByMilestoneId(milestoneId: string): Observable<Issue[]> {
    return this.appService.get<Issue[]>(this.appService.createAPIUrl(`issues/milestone/${milestoneId}`));
  }
}
