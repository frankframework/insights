import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from '../app.service';

export interface Release {
  id: string;
  tagName: string;
  name: string;
  publishedAt: Date;
  commitSha: string;
  branch: Branch;
}

interface Branch {
  id: string;
  name: string;
}

@Injectable({
  providedIn: 'root',
})
export class ReleaseService {
  constructor(private appService: AppService) {}

  public getAllReleases(): Observable<Release[]> {
    return this.appService.get<Release[]>(this.appService.createAPIUrl('releases'));
  }
}
