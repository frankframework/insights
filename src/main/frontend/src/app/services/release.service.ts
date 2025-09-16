import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AppService } from '../app.service';

export type Release = {
  id: string;
  tagName: string;
  name: string;
  publishedAt: Date | string;
  branch: Branch;
};

export type Branch = {
  id: string;
  name: string;
};

@Injectable({
  providedIn: 'root',
})
export class ReleaseService {
  private appService = inject(AppService);

  public getAllReleases(): Observable<Release[]> {
    return this.appService.get<Release[]>(this.appService.createAPIUrl('releases'));
  }
}
