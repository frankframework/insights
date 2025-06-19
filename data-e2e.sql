-- Branches
INSERT INTO branch (id, name) VALUES
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

-- Releases
INSERT INTO release (id, tag_name, name, published_at, branch_id) VALUES
('RE_kwDOAIg5ds4H2uLY','v7.8.4','v7.8.4','2023-11-28T18:16:14Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
('RE_kwDOAIg5ds4FCsYR','v7.7.5','v7.7.5','2022-11-30T15:29:53Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
('RE_kwDOAIg5ds4MwqlG','release/9.1-nightly','v9.1.1-20250612.022341 (nightly)','2025-06-12T01:52:04Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvOS4x'),
('MDc6UmVsZWFzZTE5MTg1NjMx','v7.4','v7.4','2019-08-09T08:29:49Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg=='),
('MDc6UmVsZWFzZTM5OTQ3Mjk3','v7.6-RC1','v7.6-RC1','2021-03-17T14:49:31Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
('RE_kwDOAIg5ds4JT_pm','v7.9.3','v7.9.3','2024-05-17T10:02:56Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
('RE_kwDOAIg5ds4IC5Zw','v8.0.0','v8.0.0','2023-12-23T14:23:48Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
('RE_kwDOAIg5ds4J18zh','v8.2.0','v8.2.0','2024-07-12T10:51:35Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMi1yZWxlYXNl'),
('RE_kwDOAIg5ds4Ezmke','v7.8-RC2','v7.8-RC2','2022-10-21T16:14:38Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
('RE_kwDOAIg5ds4L1QJF','7.9-nightly','v7.9.7-20250612.065044 (nightly)','2025-06-12T06:37:38Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
('RE_kwDOAIg5ds4I8K7R','v8.1.0-RC1','v8.1.0-RC1','2024-04-05T17:42:50Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
('RE_kwDOAIg5ds4DVcfu','v7.6.3','v7.6.3','2021-12-24T13:45:29Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
('RE_kwDOAIg5ds4HUdA_','v7.8.2','v7.8.2','2023-09-27T15:22:22Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
('RE_kwDOAIg5ds4Hlplu','v7.9-RC1','v7.9-RC1','2023-10-31T08:27:26Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
('RE_kwDOAIg5ds4EQLJy','v7.6.5','v7.6.5','2022-07-06T13:05:56Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
('MDc6UmVsZWFzZTExMjgwNTk1','v7.0-RC3','v7.0-RC3','2018-06-01T13:45:01Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
('MDc6UmVsZWFzZTM0MDA2OTE2','v7.5','v7.5','2020-11-16T14:51:47Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy41'),
('MDc6UmVsZWFzZTQ0MDQ1Mjcw','v7.6-RC2','v7.6-RC2','2021-06-03T13:57:22Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
('MDc6UmVsZWFzZTczNjI2ODg=','v7.0-B2','v7.0-B2','2017-08-11T15:43:20Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
('RE_kwDOAIg5ds4KrJiA','v8.1.2','v8.1.2','2024-10-09T11:08:40Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
('RE_kwDOAIg5ds4JdZVZ','8.0-nightly','v8.0.6-20250305.000628 (nightly)','2025-03-05T00:18:18Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
('RE_kwDOAIg5ds4FkWRa','v7.8-RC3','v7.8-RC3','2023-02-23T09:39:15Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
('RE_kwDOAIg5ds4JWcoo','v8.1.0','v8.1.0','2024-05-22T13:07:18Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
('RE_kwDOAIg5ds4HeiTd','v7.8.3','v7.8.3','2023-10-17T15:09:51Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
('RE_kwDOAIg5ds4Fj6Z-','v7.7.6','v7.7.6','2023-02-22T14:18:00Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
('RE_kwDOAIg5ds4KrJui','v8.2.1','v8.2.1','2024-10-09T11:13:58Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMi1yZWxlYXNl'),
('MDc6UmVsZWFzZTc4MDkyNjQ=','v7.0-B3','v7.0-B3','2017-09-19T14:46:54Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
('RE_kwDOAIg5ds4JaYPG','master-nightly','v9.2.0-20250613.042336 (nightly)','2025-06-13T04:06:01Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg=='),
('RE_kwDOAIg5ds4DsJ4k','v7.7-RC2','v7.7-RC2','2022-03-15T16:48:54Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
('RE_kwDOAIg5ds4H-_so','v7.8.5','v7.8.5','2023-12-14T14:37:24Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
('RE_kwDOAIg5ds4I9mAj','v7.9.2','v7.9.2','2024-04-09T10:04:40Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
('RE_kwDOAIg5ds4KrI8p','v7.9.5','v7.9.5','2024-10-09T10:58:07Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
('MDc6UmVsZWFzZTI0NTExODMx','v7.5-RC2','v7.5-RC2','2020-03-13T17:53:11Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy41'),
('MDc6UmVsZWFzZTE4NTQ2MjE3','v7.3','v7.3','2019-07-11T08:56:45Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4z'),
('MDc6UmVsZWFzZTQ5MDUxNjU=','v6.1','v6.1','2016-12-13T12:35:30Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg=='),
('RE_kwDOAIg5ds4H_D0j','v7.9.0','v7.9.0','2023-12-14T16:11:58Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
('RE_kwDOAIg5ds4DFmai','v7.6.1','v7.6.1','2021-10-21T15:12:48Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
('RE_kwDOAIg5ds4JUJAU','v8.0.2','v8.0.2','2024-05-17T13:49:57Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
('RE_kwDOAIg5ds4KLQbP','v8.0.3','v8.0.3','2024-08-17T16:21:00Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
('RE_kwDOAIg5ds4EiaNU','v7.8-RC1','v7.8-RC1','2022-09-02T09:24:55Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
('RE_kwDOAIg5ds4Ks6Gh','v8.3.0','v8.3.0','2024-10-11T15:12:14Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMy1yZWxlYXNl'),
('RE_kwDOAIg5ds4Ky4-S','release/8.3-nightly','v8.3.3-20250605.001628 (nightly)','2025-06-04T23:37:27Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMy1yZWxlYXNl'),
('RE_kwDOAIg5ds4Lhg3o','v9.0.0','v9.0.0','2025-01-07T14:29:41Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),
('RE_kwDOAIg5ds4MnUo_','v9.0.1','v9.0.1','2025-04-10T17:36:45Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),
('RE_kwDOAIg5ds4LkXe7','release/9.0-nightly','v9.0.2-20250612.031144 (nightly)','2025-06-12T02:33:56Z','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w');

INSERT INTO issue_type (id, name, description, color) VALUES
('type-epic', 'Epic', 'A large body of work that can be broken down into a number of smaller stories', '9400D3'),
('type-feature', 'Feature', 'A new feature or enhancement for the user', '4169E1'),
('type-bug', 'Bug', 'A bug fix or correction of an error', 'B22222'),
('type-task', 'Task', 'A technical task or chore', '778899')
ON CONFLICT (id) DO NOTHING;

INSERT INTO label (id, name, description, color) VALUES
('label-perf', 'performance', 'Improvements related to performance', 'f29513'),
('label-sec', 'security', 'Improvements related to security', 'bf251a'),
('label-ui', 'ui/ux', 'Improvements related to user interface and experience', '0e8a16'),
('label-ci', 'ci/cd', 'Changes related to continuous integration and deployment', '5319e7')
ON CONFLICT (id) DO NOTHING;

INSERT INTO issue (id, number, title, state, url, issue_type_id) VALUES
('issue-epic-101', 101, 'Epic: Overhaul the Dashboard UI', 1, 'http://example.com/issues/101', 'type-epic');

INSERT INTO issue (id, number, title, state, url, issue_type_id) VALUES
('issue-task-102', 102, 'Task: Create new icon set for dashboard widgets', 1, 'http://example.com/issues/102', 'type-task'),
('issue-feat-103', 103, 'Feature: Add real-time graphing widget', 1, 'http://example.com/issues/103', 'type-feature'),
('issue-bug-104', 104, 'Bug: Widget alignment is broken on Firefox', 1, 'http://example.com/issues/104', 'type-bug');

INSERT INTO issue (id, number, title, state, url, issue_type_id) VALUES
('issue-feat-105', 105, 'Feature: Implement API key authentication for security', 1, 'http://example.com/issues/105', 'type-feature');

INSERT INTO issue_sub_issues (issue_id, sub_issues_id) VALUES
('issue-epic-101', 'issue-task-102'), 
('issue-epic-101', 'issue-feat-103'),
('issue-epic-101', 'issue-bug-104');

INSERT INTO issue_label (id, issue_id, label_id) VALUES
('9446a7b9-85bb-41e9-ba2f-f97a236d75e2', 'issue-epic-101', 'label-ui'),
('0c0e9bfb-5ab7-437c-b320-874b5c380674', 'issue-task-102', 'label-ui'),
('17483681-a653-4344-aea4-1eb1f4a2bcf3', 'issue-feat-103', 'label-ui'),
('23e34af1-984a-4f88-a6c8-73400051e2ea', 'issue-feat-103', 'label-perf'),
('f16f82d2-cda4-4345-851a-d810a115a929', 'issue-bug-104', 'label-ui'),
('4ca5380b-f58b-4083-bd7b-21cfbb9932e9', 'issue-feat-105', 'label-sec');

INSERT INTO pull_request (id, number, title, url, merged_at) VALUES
('pr-501', 501, 'feat(ui): Add new graphing widget and icon set', 'http://example.com/pulls/501', '2025-04-08T10:00:00Z'),
('pr-502', 502, 'fix(css): Correct widget alignment on Firefox', 'http://example.com/pulls/502', '2025-04-09T11:00:00Z'),
('pr-503', 503, 'feat(auth): Add API key authentication middleware', 'http://example.com/pulls/503', '2025-04-09T15:00:00Z');

INSERT INTO pull_request_issue (id, pull_request_id, issue_id) VALUES
('4c74fb21-842e-44cd-8759-e0e62b85c38e', 'pr-501', 'issue-feat-103'),
('2b723e77-94f0-4489-9ab6-029df81e3e82', 'pr-501', 'issue-task-102'),
('8db9d19e-cc64-4f9a-ac8f-103080d9cfa7', 'pr-502', 'issue-bug-104'),
('cfd2ff4c-d15c-4e56-8f62-8cfee5ef7039', 'pr-503', 'issue-feat-105');

INSERT INTO release_pull_request (id, release_id, pull_request_id) VALUES
('9d86f21a-a641-4204-961f-2730cb352f47', 'RE_kwDOAIg5ds4MnUo_', 'pr-501'),
('343af7e5-45b3-47eb-90f6-114096f69dc3', 'RE_kwDOAIg5ds4MnUo_', 'pr-502'),
('1d5ca201-ea87-49c9-985a-2b912dc9f220', 'RE_kwDOAIg5ds4MnUo_', 'pr-503');
