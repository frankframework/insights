import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface ActuatorInfo {
  build?: BuildInfo;
}

export interface BuildInfo {
  version?: string;
  time?: string;
}

@Injectable({
  providedIn: 'root',
})
export class VersionService {
  private actuatorUrl = '/actuator/info';

  private http = inject(HttpClient);

  /**
   * Fetches the build information from the backend.
   * Returns null if the request fails or version is missing.
   */
  getBuildInformation(): Observable<BuildInfo | null> {
    return this.http.get<ActuatorInfo>(this.actuatorUrl).pipe(
      map((response) => {
        return response?.build || null;
      }),
      catchError((error) => {
        console.warn('Failed to fetch version, defaulting to null', error);
        return of(null);
      }),
    );
  }
}
