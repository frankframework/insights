import { Injectable, inject } from '@angular/core';
import { AppService } from '../app.service';
import { Observable } from 'rxjs';

export interface BusinessValue {
  id: string;
  title: string;
  description: string;
  issueNumber?: number;
  issueUrl?: string;
}

@Injectable({
  providedIn: 'root',
})
export class BusinessValueService {
  private appService = inject(AppService);

  public getBusinessValuesByReleaseId(releaseId: string): Observable<BusinessValue[]> {
    return this.appService.get<BusinessValue[]>(this.appService.createAPIUrl(`business-values/release/${releaseId}`));
  }
}
