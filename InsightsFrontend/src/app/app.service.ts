import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {Observable, Timestamp} from 'rxjs';

export const GitHubState = {
	OPEN: 0,
	CLOSED: 1,
	MERGED: 2
} as const;

export type GitHubState = typeof GitHubState[keyof typeof GitHubState];

export type ApiResponse<T> = {
	status: number;
	message: string;
	body: T;
}

@Injectable({
	providedIn: 'root'
})
export class AppService {
	private API_BASE_URL: string = "http://localhost:8080/api";

	constructor(private http: HttpClient) {}

	/**
	 * Get a collection of items.
	 * @param url The API endpoint
	 * @param params Optional query parameters to call requests that use query parameters.
	 */
	public getAll<T>(url: string, params?: Record<string, string | number>): Observable<Record<string, ApiResponse<T>>> {
		return this.http.get<Record<string, ApiResponse<T>>>(url, { params });
	}


	/**
	 * Get an endpoint URL.
	 * @param endpoint The string that extends the base url to reach a specific endpoint
	 */
	public createAPIUrl(endpoint: string): string {
		return `${this.API_BASE_URL}/${endpoint}`;
	}
}
