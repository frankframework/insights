import { Injectable } from '@angular/core';
import {Observable} from "rxjs";
import {ApiResponse, AppService} from "../app.service";

export type Release = {
	id: string;
	tagName: string;
	name: string;
	publishedAt: Date;
	commitSha: string;
	branch: Branch;
}

type Branch = {
	id: string;
	name: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReleaseService {
	constructor(private appService: AppService) { }

	getAllReleases(): Observable<Record<string, ApiResponse<Release[]>>> {
		return this.appService.getAll<Release[]>(this.appService.createAPIUrl('releases'));
	}
}
