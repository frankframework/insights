import {Status} from "./status.enum";

export interface Milestone {
	id: string;
	number: number;
	title: string;
	state: Status;
}
