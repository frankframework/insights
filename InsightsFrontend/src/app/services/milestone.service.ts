import { Injectable } from '@angular/core';
import { AppService, GitHubState } from '../app.service';
import { Observable } from 'rxjs';

export interface Milestone {
  id: string;
  number: number;
  title: string;
  url: string;
  state: GitHubState;
  dueOn: Date | null;
  openIssueCount: number;
  closedIssueCount: number;
  isEstimated?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class MilestoneService {
  constructor(private appService: AppService) {}

  public getOpenMilestones(): Observable<Milestone[]> {
    return this.appService.get<Milestone[]>(this.appService.createAPIUrl('milestones/open'));
  }
}
