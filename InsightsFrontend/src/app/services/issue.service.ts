import { Injectable } from '@angular/core';
import {ApiResponse, AppService, GitHubState} from "../app.service";
import {Milestone} from "./milestone.service";
import {Label} from "./label.service";
import {Observable} from "rxjs";

export type Issue = {
	id: string;
	number: number;
	title: string;
	state: GitHubState;
	closedAt: Date;
	url: string;
	businessValue: string;
	milestone: Milestone;
	labels: Label[];
	parentIssue: Issue;
	subIssues: Issue[];
}

type TimeSpanParams = {
	startDate: string;
	endDate: string;
};

@Injectable({
  providedIn: 'root'
})
export class IssueService {

	constructor(private appService: AppService) { }

	public getIssuesByReleaseId(releaseId: string): Observable<Record<string, ApiResponse<Issue[]>>> {
		return this.appService.getAll<Issue[]>(this.appService.createAPIUrl("/issues/release/" + releaseId));
	}

	public getIssuesByMilestoneId(milestoneId: string): Observable<Record<string, ApiResponse<Issue[]>>> {
		return this.appService.getAll<Issue[]>(this.appService.createAPIUrl("/issues/milestone/" + milestoneId));
	}

	public getIssuesByTimeSpan(startDate: Date, endDate: Date): Observable<Record<string, ApiResponse<Issue[]>>> {
		const url = this.appService.createAPIUrl("issues/timespan");

		const timestampParams: TimeSpanParams = {
			startDate: startDate.toISOString(),
			endDate: endDate.toISOString()
		};

		return this.appService.getAll<Issue[]>(url, timestampParams);
	}
}
