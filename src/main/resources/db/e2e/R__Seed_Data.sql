-- This seeder is compatible with both H2 and PostgreSQL.

MERGE INTO branch (id, name) KEY(id) VALUES
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44', 'release/7.8'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43', 'release/7.7'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvOS4x', 'release/9.1'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg==', 'master'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42', 'release/7.6'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl', '7.9-release'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl', '8.0-release'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMi1yZWxlYXNl', '8.2-release'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl', '8.1-release'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMy1yZWxlYXNl', 'release/8.3'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w', 'release/7.0'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy41', 'release/7.5'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4z', 'release/7.3'),
	('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w', 'release/9.0');

MERGE INTO milestone (id, number, title, state, url, due_on, open_issue_count, closed_issue_count) KEY(id) VALUES
	('milestone-past', 10, 'Release 9.3.0', 0, 'http://example.com/milestone/10', DATEADD('MONTH', -1, CURRENT_TIMESTAMP()), 1, 1),
	('milestone-current', 11, 'Release 9.4.0', 0, 'http://example.com/milestone/11', DATEADD('MONTH', 2, CURRENT_TIMESTAMP()), 2, 2),
	('milestone-future', 12, 'Release 9.5.0', 0, 'http://example.com/milestone/12', DATEADD('MONTH', 5, CURRENT_TIMESTAMP()), 2, 0),
	('milestone-overflow', 13, 'Release 9.6.0 (Overflow)', 0, 'http://example.com/milestone/13', DATEADD('MONTH', 2, CURRENT_TIMESTAMP()), 20, 0),
	('milestone-no-issues', 14, 'Release 9.7.0 (No Issues)', 0, 'http://example.com/milestone/14', DATEADD('MONTH', 2, CURRENT_TIMESTAMP()), 0, 0);

MERGE INTO release (id, tag_name, name, published_at, branch_id) KEY(id) VALUES
	('RE_kwDOAIg5ds4MwqlG','release/9.1-nightly','v9.1.1-nightly', CURRENT_TIMESTAMP(),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvOS4x'),
	('RE_kwDOAIg5ds4JaYPG','master-nightly','v9.2.0-nightly', CURRENT_TIMESTAMP(),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg=='),
	('RE_kwDOAIg5ds4Lhg3o','v9.0.0','v9.0.0', DATEADD('MONTH', -7, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),
	('RE_kwDOAIg5ds4MnUo_','v9.0.1','v9.0.1', DATEADD('MONTH', -4, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),
	('RE_kwDOAIg5ds4LkXe7','v9.0.2-nightly','v9.0.2-20251030.042333 (nightly)', CURRENT_TIMESTAMP(),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),
	('RE_kwDOAIg5ds4IC5Zw','v8.0.0','v8.0.0', DATEADD('MONTH', -14, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
	('RE_kwDOAIg5ds4JdZVZ','8.0-nightly','v8.0.6-nightly', DATEADD('MONTH', -13, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
	('RE_kwDOAIg5ds4JUJAU','v8.0.2','v8.0.2', DATEADD('MONTH', -12, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
	('RE_kwDOAIg5ds4KLQbP','v8.0.3','v8.0.3', CURRENT_TIMESTAMP(),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
	('RE_kwDOAIg5ds4I8K7R','v8.1.0-RC1','v8.1.0-RC1', DATEADD('MONTH', -10, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
	('RE_kwDOAIg5ds4JWcoo','v8.1.0','v8.1.0', DATEADD('MONTH', -10, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
	('RE_kwDOAIg5ds4KrJiA','v8.1.2','v8.1.2', DATEADD('MONTH', -8, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
	('MDc6UmVsZWFzZTExMjgwNTk1','v7.0-RC3','v7.0-RC3', DATEADD('MONTH', -40, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
	('MDc6UmVsZWFzZTczNjI2ODg=','v7.0-B2','v7.0-B2', DATEADD('MONTH', -42, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
	('MDc6UmVsZWFzZjc4MDkyNjQ=','v7.0-B3','v7.0-B3', DATEADD('MONTH', -41, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
	('MDc6UmVsZWFzZTE4NTQ2MjE3','v7.3','v7.3', DATEADD('MONTH', -38, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4z'),
	('MDc6UmVsZWFzZTE5MTg1NjMx','v7.4','v7.4', DATEADD('MONTH', -37, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg=='),
	('MDc6UmVsZWFzZTI0NTExODMx','v7.5-RC2','v7.5-RC2', DATEADD('MONTH', -36, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy41'),
	('MDc6UmVsZWFzZTM0MDA2OTE2','v7.5','v7.5', DATEADD('MONTH', -35, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy41'),
	('MDc6UmVsZWFzZTM5OTQ3Mjk3','v7.6-RC1','v7.6-RC1', DATEADD('MONTH', -34, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
	('MDc6UmVsZWFzZTQ0MDQ1Mjcw','v7.6-RC2','v7.6-RC2', DATEADD('MONTH', -33, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
	('RE_kwDOAIg5ds4DFmai','v7.6.1','v7.6.1', DATEADD('MONTH', -32, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
	('RE_kwDOAIg5ds4DVcfu','v7.6.2','v7.6.2', DATEADD('MONTH', -31, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
	('RE_kwDOAIg5ds4EQLJy','v7.6.3','v7.6.3', DATEADD('MONTH', -30, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
	('RE_kwDOAIg5ds4DsJ4k','v7.7-RC2','v7.7-RC2', DATEADD('MONTH', -29, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
	('RE_kwDOAIg5ds4FCsYR','v7.7.5','v7.7.5', DATEADD('MONTH', -28, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
	('RE_kwDOAIg5ds4Fj6Z-','v7.7.6','v7.7.6', DATEADD('MONTH', -27, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
	('RE_kwDOAIg5ds4EiaNU','v7.8-RC1','v7.8-RC1', DATEADD('MONTH', -26, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
	('RE_kwDOAIg5ds4Ezmke','v7.8-RC2','v7.8-RC2', DATEADD('MONTH', -25, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
	('RE_kwDOAIg5ds4FkWRa','v7.8-RC3','v7.8-RC3', DATEADD('MONTH', -24, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
	('RE_kwDOAIg5ds4HUdA_','v7.8.2','v7.8.2', DATEADD('MONTH', -23, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
	('RE_kwDOAIg5ds4HeiTd','v7.8.3','v7.8.3', DATEADD('MONTH', -22, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
	('RE_kwDOAIg5ds4H2uLY','v7.8.4','v7.8.4', DATEADD('MONTH', -21, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
	('RE_kwDOAIg5ds4H-_so','v7.8.5','v7.8.5', DATEADD('MONTH', -20, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
	('RE_kwDOAIg5ds4Hlplu','v7.9-RC1','v7.9-RC1', DATEADD('MONTH', -19, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
	('RE_kwDOAIg5ds4H_D0j','v7.9.0','v7.9.0', DATEADD('MONTH', -18, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
	('RE_kwDOAIg5ds4I9mAj','v7.9.2','v7.9.2', DATEADD('MONTH', -17, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
	('RE_kwDOAIg5ds4JT_pm','v7.9.3','v7.9.3', DATEADD('MONTH', -16, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
	('RE_kwDOAIg5ds4KrI8p','v7.9.4','v7.9.4', DATEADD('MONTH', -15, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
	('RE_kwDOAIg5ds4L1QJF','7.9-nightly','v7.9.7-nightly', DATEADD('MONTH', -15, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
	('MDc6UmVsZWFzZTQ5MDUxNjU=','v6.1','v6.1', DATEADD('MONTH', -48, CURRENT_TIMESTAMP()),'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg==');

MERGE INTO issue_type (id, name, description, color) KEY(id) VALUES
	('type-epic', 'Epic', 'A large body of work that can be broken down into a number of smaller stories', '9400D3'),
	('type-feature', 'Feature', 'A new feature or enhancement for the user', '4169E1'),
	('type-bug', 'Bug', 'A bug fix or correction of an error', 'B22222'),
	('type-task', 'Task', 'A technical task or chore', '778899');

MERGE INTO issue_priority (id, name, description, color) KEY(id) VALUES
	('prio-critical', 'Critical', '', 'RED'),
	('prio-high', 'High', '', 'YELLOW'),
	('prio-medium', 'Medium', '', 'PURPLE'),
	('prio-low', 'Low', '', 'BLUE'),
	('prio-no', 'No', '', 'GRAY');

MERGE INTO label (id, name, description, color) KEY(id) VALUES
	('label-perf', 'performance', 'Improvements related to performance', 'FEF2C0'),
	('label-sec', 'security', 'Improvements related to security', 'D4C5F9'),
	('label-ui', 'ui/ux', 'Improvements related to user interface and experience', '006B75'),
	('label-ci', 'ci/cd', 'Changes related to continuous integration and deployment', '006B75');

MERGE INTO issue (id, number, title, state, url, issue_type_id) KEY(id) VALUES
	('issue-epic-101', 101, 'Epic: Overhaul the Dashboard UI', 1, 'http://example.com/issues/101', 'type-epic'),
	('issue-task-102', 102, 'Task: Create new icon set for dashboard widgets', 1, 'http://example.com/issues/102', 'type-task'),
	('issue-feat-103', 103, 'Feature: Add real-time graphing widget', 1, 'http://example.com/issues/103', 'type-feature'),
	('issue-bug-104', 104, 'Bug: Widget alignment is broken on Firefox', 1, 'http://example.com/issues/104', 'type-bug'),
	('issue-feat-105', 105, 'Feature: Implement API key authentication for security', 1, 'http://example.com/issues/105', 'type-feature');

MERGE INTO issue (id, number, title, state, url, points, closed_at, milestone_id, issue_type_id, issue_priority_id) KEY(id) VALUES
	('issue-past-closed', 201, 'Old feature that was completed', 1, 'http://example.com/issue/201', 5, DATEADD('MONTH', -2, CURRENT_TIMESTAMP()), 'milestone-past', 'type-feature', NULL),
	('issue-past-open', 202, 'Overdue bug from past release', 0, 'http://example.com/issue/202', 3, NULL, 'milestone-past', 'type-bug', 'prio-high'),
	('issue-current-closed-1', 203, 'Task finished early this quarter', 1, 'http://example.com/issue/203', 2, DATEADD('DAY', -10, CURRENT_TIMESTAMP()), 'milestone-current', 'type-task', NULL),
	('issue-current-closed-2', 204, 'Bug fixed just before today', 1, 'http://example.com/issue/204', 3, DATEADD('DAY', -1, CURRENT_TIMESTAMP()), 'milestone-current', 'type-bug', NULL),
	('issue-current-open-1', 205, 'High priority feature to be done', 0, 'http://example.com/issue/205', 8, NULL, 'milestone-current', 'type-feature', 'prio-high'),
	('issue-current-open-2', 206, 'Another open task', 0, 'http://example.com/issue/206', 5, NULL, 'milestone-current', 'type-task', 'prio-medium'),
	('issue-zero-points', 207, 'Issue with zero points', 0, 'http://example.com/issue/207', 0, NULL, 'milestone-current', 'type-task', NULL),
	('issue-future-open-1', 301, 'Planning for Q4 feature', 0, 'http://example.com/issue/301', 13, NULL, 'milestone-future', 'type-feature', NULL),
	('issue-future-open-2', 302, 'Technical spike for Q4', 0, 'http://example.com/issue/302', 8, NULL, 'milestone-future', 'type-task', NULL);

MERGE INTO issue (id, number, title, state, url, points, closed_at, milestone_id, issue_type_id, issue_priority_id) VALUES
	('issue-overflow-1', 401, 'Overflow task 1', 0, 'http://example.com/issue/401', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-2', 402, 'Overflow task 2', 0, 'http://example.com/issue/402', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-3', 403, 'Overflow task 3', 0, 'http://example.com/issue/403', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-4', 404, 'Overflow task 4', 0, 'http://example.com/issue/404', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-5', 405, 'Overflow task 5', 0, 'http://example.com/issue/405', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-6', 406, 'Overflow task 6', 0, 'http://example.com/issue/406', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-7', 407, 'Overflow task 7', 0, 'http://example.com/issue/407', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-8', 408, 'Overflow task 8', 0, 'http://example.com/issue/408', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-9', 409, 'Overflow task 9', 0, 'http://example.com/issue/409', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-10', 410, 'Overflow task 10', 0, 'http://example.com/issue/410', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-11', 411, 'Overflow task 11', 0, 'http://example.com/issue/411', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-12', 412, 'Overflow task 12', 0, 'http://example.com/issue/412', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-13', 413, 'Overflow task 13', 0, 'http://example.com/issue/413', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-14', 414, 'Overflow task 14', 0, 'http://example.com/issue/414', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-15', 415, 'Overflow task 15', 0, 'http://example.com/issue/415', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-16', 416, 'Overflow task 16', 0, 'http://example.com/issue/416', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-17', 417, 'Overflow task 17', 0, 'http://example.com/issue/417', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-18', 418, 'Overflow task 18', 0, 'http://example.com/issue/418', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-19', 419, 'Overflow task 19', 0, 'http://example.com/issue/419', 10, NULL, 'milestone-overflow', 'type-task', NULL),
	('issue-overflow-20', 420, 'Overflow task 20', 0, 'http://example.com/issue/420', 10, NULL, 'milestone-overflow', 'type-task', NULL);

MERGE INTO issue_sub_issues (issue_id, sub_issues_id) KEY(issue_id, sub_issues_id) VALUES
	('issue-epic-101', 'issue-task-102'),
	('issue-epic-101', 'issue-feat-103'),
	('issue-epic-101', 'issue-bug-104');

MERGE INTO issue_label (issue_id, label_id) KEY(issue_id, label_id) VALUES
	('issue-epic-101', 'label-ui'),
	('issue-task-102', 'label-ui'),
	('issue-feat-103', 'label-ui'),
	('issue-feat-103', 'label-perf'),
	('issue-bug-104', 'label-ui'),
	('issue-feat-105', 'label-sec'),
	('issue-past-closed', 'label-perf'),
	('issue-past-open', 'label-sec'),
	('issue-current-closed-1', 'label-ui'),
	('issue-current-closed-2', 'label-ui'),
	('issue-current-open-1', 'label-perf'),
	('issue-current-open-2', 'label-ui'),
	('issue-zero-points', 'label-ci'),
	('issue-future-open-1', 'label-ui'),
	('issue-future-open-2', 'label-perf'),
	('issue-overflow-1', 'label-ui'),
	('issue-overflow-2', 'label-perf'),
	('issue-overflow-3', 'label-ui'),
	('issue-overflow-4', 'label-sec'),
	('issue-overflow-5', 'label-ui'),
	('issue-overflow-6', 'label-perf'),
	('issue-overflow-7', 'label-ui'),
	('issue-overflow-8', 'label-sec'),
	('issue-overflow-9', 'label-ui'),
	('issue-overflow-10', 'label-perf'),
	('issue-overflow-11', 'label-ui'),
	('issue-overflow-12', 'label-sec'),
	('issue-overflow-13', 'label-ui'),
	('issue-overflow-14', 'label-perf'),
	('issue-overflow-15', 'label-ui'),
	('issue-overflow-16', 'label-sec'),
	('issue-overflow-17', 'label-ui'),
	('issue-overflow-18', 'label-perf'),
	('issue-overflow-19', 'label-ui'),
	('issue-overflow-20', 'label-sec');

MERGE INTO pull_request (id, number, title, url, merged_at) KEY(id) VALUES
	('pr-501', 501, 'feat(ui): Add new graphing widget and icon set', 'http://example.com/pulls/501', DATEADD('DAY', -2, DATEADD('MONTH', -3, CURRENT_TIMESTAMP()))),
	('pr-502', 502, 'fix(css): Correct widget alignment on Firefox', 'http://example.com/pulls/502', DATEADD('DAY', -1, DATEADD('MONTH', -3, CURRENT_TIMESTAMP()))),
	('pr-503', 503, 'feat(auth): Add API key authentication middleware', 'http://example.com/pulls/503', DATEADD('MONTH', -3, CURRENT_TIMESTAMP()));

MERGE INTO pull_request_issue (pull_request_id, issue_id) KEY(pull_request_id, issue_id) VALUES
	('pr-501', 'issue-feat-103'),
	('pr-501', 'issue-task-102'),
	('pr-502', 'issue-bug-104'),
	('pr-503', 'issue-feat-105');

MERGE INTO release_pull_request (release_id, pull_request_id) KEY(release_id, pull_request_id) VALUES
	('RE_kwDOAIg5ds4MnUo_', 'pr-501'),
	('RE_kwDOAIg5ds4MnUo_', 'pr-502'),
	('RE_kwDOAIg5ds4MnUo_', 'pr-503');

MERGE INTO vulnerability (cve_id, severity, cvss_score, description) KEY(cve_id) VALUES
	('CVE-2024-0001', 'CRITICAL', 10.0, 'Critical remote code execution vulnerability allowing unauthenticated attackers to execute arbitrary code on the server. This vulnerability affects all versions prior to 9.0.1 and should be patched immediately.'),
	('CVE-2024-0002', 'CRITICAL', 9.8, 'Critical SQL injection vulnerability with a very long description that should trigger the see more button because it exceeds six lines of text when rendered in the UI component with normal font size and line height settings. This allows attackers to extract sensitive data from the database including user credentials and personal information.'),
	('CVE-2024-0003', 'HIGH', 8.5, 'High severity cross-site scripting (XSS) vulnerability that could allow attackers to inject malicious scripts into web pages viewed by other users.'),
	('CVE-2024-0004', 'HIGH', 7.5, 'High severity authentication bypass allowing unauthorized access to administrative functions.'),
	('CVE-2024-0005', 'MEDIUM', 6.0, 'Medium severity path traversal vulnerability enabling access to files outside the intended directory.'),
	('CVE-2024-0006', 'MEDIUM', 5.0, 'Medium severity information disclosure vulnerability exposing sensitive configuration data.'),
	('CVE-2024-0007', 'LOW', 3.5, 'Low severity denial of service vulnerability through resource exhaustion.'),
	('CVE-2024-0008', 'LOW', 2.1, 'Low severity insecure default configuration that could be exploited under specific conditions.');

MERGE INTO vulnerability_cwes (vulnerability_cve_id, cwes) KEY(vulnerability_cve_id, cwes) VALUES
	('CVE-2024-0001', 'CWE-78'),
	('CVE-2024-0001', 'CWE-94'),
	('CVE-2024-0002', 'CWE-89'),
	('CVE-2024-0002', 'CWE-707'),
	('CVE-2024-0003', 'CWE-79'),
	('CVE-2024-0003', 'CWE-80'),
	('CVE-2024-0004', 'CWE-287'),
	('CVE-2024-0004', 'CWE-306'),
	('CVE-2024-0005', 'CWE-22'),
	('CVE-2024-0006', 'CWE-200'),
	('CVE-2024-0007', 'CWE-400'),
	('CVE-2024-0008', 'CWE-276');

MERGE INTO release_vulnerability (release_id, vulnerability_cve_id) KEY(release_id, vulnerability_cve_id) VALUES
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0001'),
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0002'),
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0003'),
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0004'),
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0005'),
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0006'),
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0007'),
	('RE_kwDOAIg5ds4MnUo_', 'CVE-2024-0008');

