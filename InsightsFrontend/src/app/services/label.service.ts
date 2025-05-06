import { Injectable } from '@angular/core';
import {ApiResponse, AppService} from "../app.service";
import {Observable} from "rxjs";

export type Label = {
	id: string;
	name: string;
	description: string;
	color: string;
}

@Injectable({
  providedIn: 'root'
})
export class LabelService {

	constructor(private appService: AppService) {}

	public getHighLightsByReleaseId(releaseId: string): Observable<Record<string, ApiResponse<Label[]>>> {
		return this.appService.getAll<Label[]>(this.appService.createAPIUrl("/labels/release/" + releaseId));
	}
}
