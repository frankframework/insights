import {Status} from "./status.enum";
import {Milestone} from "./milestone.model";
import {Label} from "./label.model";

export interface Issue {
	id: string;
	number: number;
	title: string;
	state: Status;
	closedAt: Date;
	url: string;
	businessValue: string;
	milestone: Milestone;
	labels: Label[];
	parentIssue: Issue;
	subIssues: Issue[];
}
