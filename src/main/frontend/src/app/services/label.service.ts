import { Injectable, inject } from '@angular/core';
import { AppService } from '../app.service';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';

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
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  public getHighLightsByReleaseId(releaseId: string): Observable<Label[]> {
    return this.http.get<Label[]>(this.appService.createAPIUrl(`labels/release/${releaseId}`));
  }
}
