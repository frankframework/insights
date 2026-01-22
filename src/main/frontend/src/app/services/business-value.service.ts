import { Injectable, inject } from '@angular/core';
import { AppService } from '../app.service';
import { Observable } from 'rxjs';
import { Issue } from './issue.service';
import { HttpClient } from '@angular/common/http';

export interface BusinessValue {
  id: string;
  title: string;
  description: string;
  issues?: Issue[];
}

export interface ConnectIssuesRequest {
  issueIds: string[];
}

export interface CreateBusinessValueRequest {
  title: string;
  description: string;
}

@Injectable({
  providedIn: 'root',
})
export class BusinessValueService {
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  public getAllBusinessValues(): Observable<BusinessValue[]> {
    return this.http.get<BusinessValue[]>(this.appService.createAPIUrl('business-value'));
  }

  public getBusinessValuesByReleaseId(releaseId: string): Observable<BusinessValue[]> {
    return this.http.get<BusinessValue[]>(this.appService.createAPIUrl(`business-value/release/${releaseId}`));
  }

  public getBusinessValueById(id: string): Observable<BusinessValue> {
    return this.http.get<BusinessValue>(this.appService.createAPIUrl(`business-value/${id}`));
  }

  public createBusinessValue(title: string, description: string): Observable<BusinessValue> {
    const request: CreateBusinessValueRequest = { title, description };
    return this.http.post<BusinessValue>(this.appService.createAPIUrl('business-value'), request);
  }

  public updateBusinessValue(id: string, title: string, description: string): Observable<BusinessValue> {
    const request: CreateBusinessValueRequest = { title, description };
    return this.http.put<BusinessValue>(this.appService.createAPIUrl(`business-value/${id}`), request);
  }

  public updateIssueConnections(id: string, issueIds: string[]): Observable<BusinessValue> {
    const request: ConnectIssuesRequest = { issueIds };
    return this.http.put<BusinessValue>(this.appService.createAPIUrl(`business-value/${id}/issues`), request);
  }

  public deleteBusinessValue(id: string): Observable<void> {
    return this.http.delete<void>(this.appService.createAPIUrl(`business-value/${id}`));
  }
}
