-- Note: The syntax used here (CURRENT_TIMESTAMP, INTERVAL) is common in SQL dialects like PostgreSQL.
-- If you are using a different database (e.g., SQLite), you may need to adjust the date functions.

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
    ('MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w', 'release/9.0')
ON CONFLICT (id) DO NOTHING;

INSERT INTO milestone (id, number, title, state, url, due_on, open_issue_count, closed_issue_count) VALUES
    ('milestone-past', 10, 'Release 9.3.0', 0, 'http://example.com/milestone/10', CURRENT_TIMESTAMP - interval '1 month', 1, 1),
    ('milestone-current', 11, 'Release 9.4.0', 0, 'http://example.com/milestone/11', CURRENT_TIMESTAMP + interval '2 months', 2, 2),
    ('milestone-future', 12, 'Release 9.5.0', 0, 'http://example.com/milestone/12', CURRENT_TIMESTAMP + interval '5 months', 2, 0),
    ('milestone-overflow', 13, 'Release 9.6.0 (Overflow)', 0, 'http://example.com/milestone/13', CURRENT_TIMESTAMP + interval '2 months', 20, 0),
    ('milestone-no-issues', 14, 'Release 9.7.0 (No Issues)', 0, 'http://example.com/milestone/14', CURRENT_TIMESTAMP + interval '2 months', 0, 0)
ON CONFLICT (id) DO NOTHING;


INSERT INTO release (id, tag_name, name, published_at, branch_id) VALUES
    ('RE_kwDOAIg5ds4MwqlG','release/9.1-nightly','v9.1.1-nightly', CURRENT_TIMESTAMP,'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvOS4x'),
    ('RE_kwDOAIg5ds4JaYPG','master-nightly','v9.2.0-nightly', CURRENT_TIMESTAMP,'MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg=='),
    ('RE_kwDOAIg5ds4Lhg3o','v9.0.0','v9.0.0', CURRENT_TIMESTAMP - interval '4 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),
    ('RE_kwDOAIg5ds4MnUo_','v9.0.1','v9.0.1', CURRENT_TIMESTAMP - interval '3 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),
    ('RE_kwDOAIg5ds4LkXe7','release/9.0-nightly','v9.0.2-nightly', CURRENT_TIMESTAMP - interval '1 month','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL2hlYWRzL3JlbGVhc2UvOS4w'),

    ('RE_kwDOAIg5ds4IC5Zw','v8.0.0','v8.0.0', CURRENT_TIMESTAMP - interval '14 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'), -- Support anchor
    ('RE_kwDOAIg5ds4JdZVZ','8.0-nightly','v8.0.6-nightly', CURRENT_TIMESTAMP - interval '13 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4JUJAU','v8.0.2','v8.0.2', CURRENT_TIMESTAMP - interval '12 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4KLQbP','v8.0.3','v8.0.3', CURRENT_TIMESTAMP - interval '11 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMC1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4I8K7R','v8.1.0-RC1','v8.1.0-RC1', CURRENT_TIMESTAMP - interval '10 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4JWcoo','v8.1.0','v8.1.0', CURRENT_TIMESTAMP - interval '9 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4KrJiA','v8.1.2','v8.1.2', CURRENT_TIMESTAMP - interval '8 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4J18zh','v8.2.0','v8.2.0', CURRENT_TIMESTAMP - interval '7 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMi1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4KrJui','v8.2.1','v8.2.1', CURRENT_TIMESTAMP - interval '6 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMi1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4Ks6Gh','v8.3.0','v8.3.0', CURRENT_TIMESTAMP - interval '5 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMy1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4Ky4-S','release/8.3-nightly','v8.3.3-nightly', CURRENT_TIMESTAMP - interval '4 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzguMy1yZWxlYXNl'),

    ('MDc6UmVsZWFzZTExMjgwNTk1','v7.0-RC3','v7.0-RC3', CURRENT_TIMESTAMP - interval '40 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
    ('MDc6UmVsZWFzZTczNjI2ODg=','v7.0-B2','v7.0-B2', CURRENT_TIMESTAMP - interval '42 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
    ('MDc6UmVsZWFzZTc4MDkyNjQ=','v7.0-B3','v7.0-B3', CURRENT_TIMESTAMP - interval '41 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4w'),
    ('MDc6UmVsZWFzZTE4NTQ2MjE3','v7.3','v7.3', CURRENT_TIMESTAMP - interval '38 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy4z'),
    ('MDc6UmVsZWFzZTE5MTg1NjMx','v7.4','v7.4', CURRENT_TIMESTAMP - interval '37 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg=='),
    ('MDc6UmVsZWFzZTI0NTExODMx','v7.5-RC2','v7.5-RC2', CURRENT_TIMESTAMP - interval '36 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy41'),
    ('MDc6UmVsZWFzZTM0MDA2OTE2','v7.5','v7.5', CURRENT_TIMESTAMP - interval '35 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy41'),
    ('MDc6UmVsZWFzZTM5OTQ3Mjk3','v7.6-RC1','v7.6-RC1', CURRENT_TIMESTAMP - interval '34 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
    ('MDc6UmVsZWFzZTQ0MDQ1Mjcw','v7.6-RC2','v7.6-RC2', CURRENT_TIMESTAMP - interval '33 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
    ('RE_kwDOAIg5ds4DFmai','v7.6.1','v7.6.1', CURRENT_TIMESTAMP - interval '32 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
    ('RE_kwDOAIg5ds4DVcfu','v7.6.3','v7.6.3', CURRENT_TIMESTAMP - interval '31 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
    ('RE_kwDOAIg5ds4EQLJy','v7.6.5','v7.6.5', CURRENT_TIMESTAMP - interval '30 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy42'),
    ('RE_kwDOAIg5ds4DsJ4k','v7.7-RC2','v7.7-RC2', CURRENT_TIMESTAMP - interval '29 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
    ('RE_kwDOAIg5ds4FCsYR','v7.7.5','v7.7.5', CURRENT_TIMESTAMP - interval '28 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
    ('RE_kwDOAIg5ds4Fj6Z-','v7.7.6','v7.7.6', CURRENT_TIMESTAMP - interval '27 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy43'),
    ('RE_kwDOAIg5ds4EiaNU','v7.8-RC1','v7.8-RC1', CURRENT_TIMESTAMP - interval '26 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
    ('RE_kwDOAIg5ds4Ezmke','v7.8-RC2','v7.8-RC2', CURRENT_TIMESTAMP - interval '25 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
    ('RE_kwDOAIg5ds4FkWRa','v7.8-RC3','v7.8-RC3', CURRENT_TIMESTAMP - interval '24 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
    ('RE_kwDOAIg5ds4HUdA_','v7.8.2','v7.8.2', CURRENT_TIMESTAMP - interval '23 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
    ('RE_kwDOAIg5ds4HeiTd','v7.8.3','v7.8.3', CURRENT_TIMESTAMP - interval '22 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
    ('RE_kwDOAIg5ds4H2uLY','v7.8.4','v7.8.4', CURRENT_TIMESTAMP - interval '21 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
    ('RE_kwDOAIg5ds4H-_so','v7.8.5','v7.8.5', CURRENT_TIMESTAMP - interval '20 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL3JlbGVhc2UvNy44'),
    ('RE_kwDOAIg5ds4Hlplu','v7.9-RC1','v7.9-RC1', CURRENT_TIMESTAMP - interval '19 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4H_D0j','v7.9.0','v7.9.0', CURRENT_TIMESTAMP - interval '18 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4I9mAj','v7.9.2','v7.9.2', CURRENT_TIMESTAMP - interval '17 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4JT_pm','v7.9.3','v7.9.3', CURRENT_TIMESTAMP - interval '16 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4KrI8p','v7.9.4','v7.9.4', CURRENT_TIMESTAMP - interval '15 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),
    ('RE_kwDOAIg5ds4L1QJF','7.9-nightly','v7.9.7-nightly', CURRENT_TIMESTAMP - interval '15 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzLzcuOS1yZWxlYXNl'),

    ('MDc6UmVsZWFzZTQ5MDUxNjU=','v6.1','v6.1', CURRENT_TIMESTAMP - interval '48 months','MDM6UmVmODkyNzYwNjpyZWZzL2hlYWRzL21hc3Rlcg==')
ON CONFLICT (id) DO NOTHING;

INSERT INTO issue_type (id, name, description, color) VALUES
    ('type-epic', 'Epic', 'A large body of work that can be broken down into a number of smaller stories', '9400D3'),
    ('type-feature', 'Feature', 'A new feature or enhancement for the user', '4169E1'),
    ('type-bug', 'Bug', 'A bug fix or correction of an error', 'B22222'),
    ('type-task', 'Task', 'A technical task or chore', '778899')
ON CONFLICT (id) DO NOTHING;

INSERT INTO issue_priority (id, name, description, color) VALUES
    ('prio-critical', 'Critical', '', 'RED'),
    ('prio-high', 'High', '', 'YELLOW'),
    ('prio-medium', 'Medium', '', 'PURPLE'),
    ('prio-low', 'Low', '', 'BLUE'),
    ('prio-no', 'No', '', 'GRAY')
ON CONFLICT (id) DO NOTHING;

INSERT INTO label (id, name, description, color) VALUES
    ('label-perf', 'performance', 'Improvements related to performance', 'FEF2C0'),
    ('label-sec', 'security', 'Improvements related to security', 'D4C5F9'),
    ('label-ui', 'ui/ux', 'Improvements related to user interface and experience', '006B75'),
    ('label-ci', 'ci/cd', 'Changes related to continuous integration and deployment', '006B75')
ON CONFLICT (id) DO NOTHING;

INSERT INTO issue (id, number, title, state, url, issue_type_id) VALUES
    ('issue-epic-101', 101, 'Epic: Overhaul the Dashboard UI', 1, 'http://example.com/issues/101', 'type-epic'),
    ('issue-task-102', 102, 'Task: Create new icon set for dashboard widgets', 1, 'http://example.com/issues/102', 'type-task'),
    ('issue-feat-103', 103, 'Feature: Add real-time graphing widget', 1, 'http://example.com/issues/103', 'type-feature'),
    ('issue-bug-104', 104, 'Bug: Widget alignment is broken on Firefox', 1, 'http://example.com/issues/104', 'type-bug'),
    ('issue-feat-105', 105, 'Feature: Implement API key authentication for security', 1, 'http://example.com/issues/105', 'type-feature')
ON CONFLICT (id) DO NOTHING;

INSERT INTO issue (id, number, title, state, url, points, closed_at, milestone_id, issue_type_id, issue_priority_id) VALUES
    ('issue-past-closed', 201, 'Old feature that was completed', 1, 'http://example.com/issue/201', 5, CURRENT_TIMESTAMP - interval '2 months', 'milestone-past', 'type-feature', NULL),
    ('issue-past-open', 202, 'Overdue bug from past release', 0, 'http://example.com/issue/202', 3, NULL, 'milestone-past', 'type-bug', 'prio-high'),
    ('issue-current-closed-1', 203, 'Task finished early this quarter', 1, 'http://example.com/issue/203', 2, CURRENT_TIMESTAMP - interval '10 days', 'milestone-current', 'type-task', NULL),
    ('issue-current-closed-2', 204, 'Bug fixed just before today', 1, 'http://example.com/issue/204', 3, CURRENT_TIMESTAMP - interval '1 day', 'milestone-current', 'type-bug', NULL),
    ('issue-current-open-1', 205, 'High priority feature to be done', 0, 'http://example.com/issue/205', 8, NULL, 'milestone-current', 'type-feature', 'prio-high'),
    ('issue-current-open-2', 206, 'Another open task', 0, 'http://example.com/issue/206', 5, NULL, 'milestone-current', 'type-task', 'prio-medium'),
    ('issue-zero-points', 207, 'Issue with zero points', 0, 'http://example.com/issue/207', 0, NULL, 'milestone-current', 'type-task', NULL),
    ('issue-future-open-1', 301, 'Planning for Q4 feature', 0, 'http://example.com/issue/301', 13, NULL, 'milestone-future', 'type-feature', NULL),
    ('issue-future-open-2', 302, 'Technical spike for Q4', 0, 'http://example.com/issue/302', 8, NULL, 'milestone-future', 'type-task', NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO issue (id, number, title, state, url, points, closed_at, milestone_id, issue_type_id, issue_priority_id)
SELECT
    'issue-overflow-' || i,
    400 + i,
    'Overflow task ' || i,
    0,
    'http://example.com/issue/' || (400 + i),
    10,
    NULL,
    'milestone-overflow',
    'type-task',
    NULL
FROM generate_series(1, 20) AS i
ON CONFLICT (id) DO NOTHING;

INSERT INTO issue_sub_issues (issue_id, sub_issues_id) VALUES
    ('issue-epic-101', 'issue-task-102'),
    ('issue-epic-101', 'issue-feat-103'),
    ('issue-epic-101', 'issue-bug-104')
ON CONFLICT (issue_id, sub_issues_id) DO NOTHING;

INSERT INTO issue_label (issue_id, label_id) VALUES
    ('issue-epic-101', 'label-ui'),
    ('issue-task-102', 'label-ui'),
    ('issue-feat-103', 'label-ui'),
    ('issue-feat-103', 'label-perf'),
    ('issue-bug-104', 'label-ui'),
    ('issue-feat-105', 'label-sec')
ON CONFLICT (issue_id, label_id) DO NOTHING;

INSERT INTO pull_request (id, number, title, url, merged_at) VALUES
    ('pr-501', 501, 'feat(ui): Add new graphing widget and icon set', 'http://example.com/pulls/501', CURRENT_TIMESTAMP - interval '3 months' - interval '2 days'),
    ('pr-502', 502, 'fix(css): Correct widget alignment on Firefox', 'http://example.com/pulls/502', CURRENT_TIMESTAMP - interval '3 months' - interval '1 day'),
    ('pr-503', 503, 'feat(auth): Add API key authentication middleware', 'http://example.com/pulls/503', CURRENT_TIMESTAMP - interval '3 months')
ON CONFLICT (id) DO NOTHING;

INSERT INTO pull_request_issue (pull_request_id, issue_id) VALUES
    ('pr-501', 'issue-feat-103'),
    ('pr-501', 'issue-task-102'),
    ('pr-502', 'issue-bug-104'),
    ('pr-503', 'issue-feat-105')
ON CONFLICT (pull_request_id, issue_id) DO NOTHING;

INSERT INTO release_pull_request (release_id, pull_request_id) VALUES
    ('RE_kwDOAIg5ds4MnUo_', 'pr-501'),
    ('RE_kwDOAIg5ds4MnUo_', 'pr-502'),
    ('RE_kwDOAIg5ds4MnUo_', 'pr-503')
ON CONFLICT (release_id, pull_request_id) DO NOTHING;
