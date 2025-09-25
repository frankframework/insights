import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from '../app.service';

export interface Release {
  id: string;
  tagName: string;
  name: string;
  publishedAt: Date | string;
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
  private appService = inject(AppService);

  public getAllReleases(): Observable<Release[]> {
    return this.appService.get<Release[]>(this.appService.createAPIUrl('releases'));
  }
}
