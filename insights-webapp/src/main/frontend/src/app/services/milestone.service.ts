import { Injectable, inject } from '@angular/core';
import { AppService, GitHubState } from '../app.service';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

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
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  public getMilestones(): Observable<Milestone[]> {
    return this.http.get<Milestone[]>(this.appService.createAPIUrl('milestones'));
  }
}
