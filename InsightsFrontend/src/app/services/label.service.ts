import { Injectable } from '@angular/core';
import { AppService } from '../app.service';
import { Observable } from 'rxjs';

export interface Label {
	id: string;
	name: string;
	description: string;
	color: string;
}

@Injectable({
	providedIn: 'root',
})
export class LabelService {
	constructor(private appService: AppService) {}

	public getHighLightsByReleaseId(releaseId: string): Observable<Label[]> {
		return this.appService.get<Label[]>(this.appService.createAPIUrl(`labels/release/${releaseId}`));
	}
}
