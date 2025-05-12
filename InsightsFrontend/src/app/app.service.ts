import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable} from 'rxjs';

export const GitHubState = {
	OPEN: 0,
	CLOSED: 1,
	MERGED: 2
} as const;

export type GitHubState = typeof GitHubState[keyof typeof GitHubState];

@Injectable({
	providedIn: 'root'
})
export class AppService {
	private static readonly API_BASE_URL: string = "http://localhost:8080/api";

	constructor(private http: HttpClient) {}

	/**
	 * Get a collection of items.
	 * @param url The API endpoint
	 * @param params Optional query parameters to call requests that use query parameters.
	 */
	public get<T>(url: string, params?: Record<string, string | number>): Observable<T> {
		return this.http.get<T>(url, { params });
	}


	/**
	 * Get an endpoint URL.
	 * @param endpoint The string that extends the base url to reach a specific endpoint
	 */
	public createAPIUrl(endpoint: string): string {
		return `${AppService.API_BASE_URL}/${endpoint}`;
	}
}
