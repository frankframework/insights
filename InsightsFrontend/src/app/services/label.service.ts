import { Injectable } from '@angular/core';
import { AppService } from '../app.service';
import { Observable } from 'rxjs';

export interface Label {
  id: string;
  name: string;
  description: string;
  color: string;
}

export interface ReleaseHighlights {
  AllHighlights: Record<string, number>;
  filteredHighlights: Label[];
}

@Injectable({
  providedIn: 'root',
})
export class LabelService {
  constructor(private appService: AppService) {}

  public getHighLightsByReleaseId(releaseId: string): Observable<ReleaseHighlights> {
    return this.appService.get<ReleaseHighlights>(this.appService.createAPIUrl(`labels/release/${releaseId}`));
  }
}
