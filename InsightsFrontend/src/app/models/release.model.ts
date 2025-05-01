import {Branch} from "./branch.model";

export interface Release {
	id: string;
	tagName: string;
	name: string;
	publishedAt: Date;
	commitSha: string;
	branch: Branch;
}
