import { Injectable, inject } from '@angular/core';
import { AppService } from '../app.service';
import { Observable } from 'rxjs';
import { Issue } from './issue.service';

export interface BusinessValue {
  id: string;
  name: string;
  description: string;
  issues?: Issue[];
}

export interface ConnectIssuesRequest {
  issueIds: string[];
}

export interface CreateBusinessValueRequest {
  name: string;
  description: string;
}

@Injectable({
  providedIn: 'root',
})
export class BusinessValueService {
  private appService = inject(AppService);

  public getBusinessValuesByReleaseId(releaseId: string): Observable<BusinessValue[]> {
    return this.appService.get<BusinessValue[]>(this.appService.createAPIUrl(`business-value/release/${releaseId}`));
  }

  public getAllBusinessValues(): Observable<BusinessValue[]> {
    return this.appService.get<BusinessValue[]>(this.appService.createAPIUrl('business-value'));
  }

  public getBusinessValueById(id: string): Observable<BusinessValue> {
    return this.appService.get<BusinessValue>(this.appService.createAPIUrl(`business-value/${id}`));
  }

  public createBusinessValue(name: string, description: string): Observable<BusinessValue> {
    const request: CreateBusinessValueRequest = { name, description };
    return this.appService.post<BusinessValue>(this.appService.createAPIUrl('business-value'), request);
  }

  public updateIssueConnections(id: string, issueIds: string[]): Observable<BusinessValue> {
    const request: ConnectIssuesRequest = { issueIds };
    return this.appService.put<BusinessValue>(
      this.appService.createAPIUrl(`business-value/${id}/issues`),
      request
    );
  }
}
