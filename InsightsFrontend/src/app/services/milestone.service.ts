import { Injectable } from '@angular/core';
import {ApiResponse, AppService, GitHubState} from "../app.service";
import {Observable} from "rxjs";

export type Milestone = {
	id: string;
	number: number;
	title: string;
	state: GitHubState;
}

@Injectable({
  providedIn: 'root'
})
export class MilestoneService {

	constructor(private appService: AppService) { }

	public getOpenMilestones(): Observable<Record<string, ApiResponse<Milestone[]>>> {
		return this.appService.getAll<Milestone[]>(this.appService.createAPIUrl("/milestones/open"));
	}
}
