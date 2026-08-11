import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from '../app.service';
import { HttpClient } from '@angular/common/http';

export interface Release {
  id: string;
  tagName: string;
  name: string;
  publishedAt: Date;
  lastScanned: Date;
  branch: Branch;
}

export interface Branch {
  id?: string;
  name: string;
}

@Injectable({
  providedIn: 'root',
})
export class ReleaseService {
  private readonly http: HttpClient = inject(HttpClient);
  private readonly appService: AppService = inject(AppService);

  public getAllReleases(): Observable<Release[]> {
    return this.http.get<Release[]>(this.appService.createAPIUrl('releases'));
  }

  public getReleaseById(releaseId: string): Observable<Release> {
    return this.http.get<Release>(this.appService.createAPIUrl(`releases/${releaseId}`));
  }
}
