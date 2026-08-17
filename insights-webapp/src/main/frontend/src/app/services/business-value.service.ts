import { Injectable, inject } from '@angular/core';
import { AppService } from '../app.service';
import { Observable } from 'rxjs';
import { Issue } from './issue.service';
import { HttpClient } from '@angular/common/http';

export interface BusinessValue {
  id: string;
  title: string;
  description: string;
  releaseId: string;
  issues?: Issue[];
}

export interface ConnectIssuesRequest {
  issueIds: string[];
}

export interface CreateBusinessValueRequest {
  title: string;
  description: string;
  releaseId: string;
}

export interface DuplicateBusinessValuesRequest {
  sourceReleaseId: string;
}

@Injectable({
  providedIn: 'root',
})
export class BusinessValueService {
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  public getBusinessValuesByReleaseId(releaseId: string): Observable<BusinessValue[]> {
    return this.http.get<BusinessValue[]>(this.appService.createAPIUrl(`business-value/release/${releaseId}`));
  }

  public getBusinessValueById(id: string): Observable<BusinessValue> {
    return this.http.get<BusinessValue>(this.appService.createAPIUrl(`business-value/${id}`));
  }

  public createBusinessValue(title: string, description: string, releaseId: string): Observable<BusinessValue> {
    const request: CreateBusinessValueRequest = { title, description, releaseId };
    return this.http.post<BusinessValue>(this.appService.createAPIUrl('business-value'), request);
  }

  public updateBusinessValue(id: string, title: string, description: string): Observable<BusinessValue> {
    return this.http.put<BusinessValue>(this.appService.createAPIUrl(`business-value/${id}`), { title, description });
  }

  public updateIssueConnections(id: string, issueIds: string[]): Observable<BusinessValue> {
    const request: ConnectIssuesRequest = { issueIds };
    return this.http.put<BusinessValue>(this.appService.createAPIUrl(`business-value/${id}/issues`), request);
  }

  public deleteBusinessValue(id: string): Observable<void> {
    return this.http.delete<void>(this.appService.createAPIUrl(`business-value/${id}`));
  }

  public duplicateBusinessValues(targetReleaseId: string, sourceReleaseId: string): Observable<BusinessValue[]> {
    const request: DuplicateBusinessValuesRequest = { sourceReleaseId };
    return this.http.post<BusinessValue[]>(
      this.appService.createAPIUrl(`business-value/release/${targetReleaseId}/duplicate`),
      request,
    );
  }
}
