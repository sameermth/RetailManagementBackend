# Dashboard, Reports, and Notifications UX Design

## Outcomes

- make the home dashboard useful for each role instead of generic
- provide a reports area that supports both on-demand exploration and scheduled reporting
- keep notifications actionable by linking them back to the exact entity or module
- give templates and schedules their own management spaces so the inbox stays simple

## Primary Users

- owner
- admin
- accountant
- store manager
- platform admin for platform-specific dashboards

## Information Architecture

Primary navigation:
- dashboard
- reports
- notifications

Admin or settings sub-navigation:
- notification templates
- report schedules

Recommended structure:
- dashboard is role-aware
- reports is analysis-oriented
- notifications is action-oriented

## Screen Inventory

### Dashboard Home

Purpose:
- first stop after login for most users

Recommended regions:
- KPI strip
- priority alerts
- sales and finance widgets
- inventory and service widgets
- recent activity feed

### Reports Landing

Purpose:
- gateway to on-demand reports and scheduled reports

Sections:
- quick report cards
- recent runs later
- scheduled reports
- category navigation if needed

### Report Runner

Purpose:
- run a selected report with filters

### Report Schedule List

Purpose:
- manage saved recurring reports

### Report Schedule Create or Edit

Purpose:
- configure recurring report generation

### Notification Inbox

Purpose:
- actionable user inbox

### Notification Detail

Purpose:
- expanded notification context with entity link

### Notification Template Manager

Purpose:
- maintain reusable notification content

### Send or Schedule Notification Tool

Purpose:
- admin-facing internal communication screen

## List Page Blueprint

### Report Schedule List

Columns:
- schedule id
- report type
- scope summary
- active
- next run
- last run
- created by

Filters:
- report type
- active
- created by
- next run period later

Top actions:
- create schedule
- run report now

Row actions:
- view
- edit
- activate or deactivate
- run now
- delete

### Notification Inbox

Columns:
- title
- notification type
- channel
- created at
- read state
- status
- linked reference

Filters:
- unread
- type
- channel
- status
- date range

Top actions:
- mark all as read

Row actions:
- open
- mark read

### Notification Template Manager

Columns:
- code
- name
- type
- channel
- active

Filters:
- type
- channel
- active
- search by code or name

Top actions:
- create template

Row actions:
- view
- edit
- activate or deactivate
- preview

## Detail Page Blueprint

### Dashboard Home

KPI strip examples:
- today sales
- outstanding receivables
- low stock items
- open service tickets

Priority alerts panel:
- overdue invoices
- low stock
- failed schedules
- unresolved claims

Activity feed:
- quote created
- invoice paid
- purchase receipt posted
- service ticket closed

UX rule:
- widget order should adapt by role
- owner sees business summary first
- cashier sees transactions first
- accountant sees dues and finance first

### Report Runner

Header:
- report name
- description

Filter panel:
- date range
- organization or branch
- customer or supplier if relevant
- product or category if relevant

Main result area:
- table preview
- summary cards
- export actions

Actions:
- run report
- export
- save as schedule

### Notification Detail

Header:
- title
- type
- created at
- channel
- read state

Sections:
- message body
- source entity
- action suggestion if any

Actions:
- open linked entity
- mark as read

## Create or Edit Flow Blueprint

### Report Schedule Create

Step 1 report selection:
- choose report type

Step 2 filter definition:
- set organization, branch, date range, and other report filters

Step 3 schedule:
- frequency
- run time
- active toggle

Step 4 delivery later if supported:
- channel or destination

Step 5 review:
- summary of report and schedule

### Notification Template Create

Sections:
- template identity: code, name
- notification type
- channel
- active state
- body or payload preview

### Send or Schedule Notification

Sections:
- target audience
- template
- payload or merge data
- send now or schedule later

## State Handling

Dashboard:
- first-use empty state for fresh organizations
- loading skeletons per widget
- partial widget failure without breaking whole dashboard

Reports:
- no data for selected filters
- long-running report loading state
- scheduled report inactive
- schedule failed last run

Notifications:
- empty inbox
- unread only state
- template inactive warning

Permission and subscription states:
- disable schedule management if user lacks permission
- disable premium reporting features with upgrade guidance

## CTA Map

Dashboard:
- primary: context-sensitive quick action based on role
- secondary: open full report or source module

Reports landing:
- primary: Run report
- secondary: Create schedule

Schedule list:
- primary: Create schedule
- row primary: View
- row secondary: Run now or Edit

Notification inbox:
- row primary: Open
- row secondary: Mark read

Template manager:
- primary: Create template
- row primary: View
- row secondary: Edit

## Desktop and Mobile Notes

Desktop:
- dashboard should support multi-widget grid
- report runner needs wide table support
- schedule and template management are desktop-first

Mobile:
- dashboard cards should collapse into a clean vertical feed
- inbox should behave like a familiar notification center
- report schedule management can be simplified to view and quick toggles

Tablet:
- very good for dashboard review and inbox triage

## Cross-Module Navigation

- dashboard widget to source invoice, purchase, stock, or service screen
- report output rows to entity detail
- notification row to linked sales, purchase, service, finance, or platform-admin entity
- schedule list to report runner

## Key Forms And Backend Fields

### Report Schedule Create Or Edit

- `scheduleName`
- `reportType`
- `format`
- `frequency`
- `cronExpression`
- `startDate`
- `endDate`
- `parameters`
- `recipients`
- `description`

### Notification Send Or Schedule

- `userId`
- `customerId`
- `supplierId`
- `distributorId`
- `type`
- `channel`
- `priority`
- `title`
- `content`
- `recipient`
- `sender`
- `referenceType`
- `referenceId`
- `scheduledFor`
- `data`

### Notification Template Create Or Edit

- `templateCode`
- `name`
- `description`
- `type`
- `channel`
- `subject`
- `content`
- `contentHtml`
- `smsContent`
- `pushTitle`
- `pushContent`
- `placeholders`
- `isActive`

## Screen To API Map

- Dashboard widgets:
  - `GET /api/dashboard/summary`
  - `GET /api/dashboard/sales/today`
  - `GET /api/dashboard/sales/period?startDate=...&endDate=...`
  - `GET /api/dashboard/products/top?limit=...`
  - `GET /api/dashboard/inventory/low-stock`
  - `GET /api/dashboard/activities/recent?limit=...`
  - `GET /api/dashboard/dues/summary`
  - `GET /api/dashboard/dues/upcoming?days=...`
  - `GET /api/dashboard/profitability?startDate=...&endDate=...&limit=...`
  - `GET /api/dashboard/aging?asOfDate=...`
  - `GET /api/dashboard/inventory/stock-summary?limit=...`
  - `GET /api/dashboard/tax/summary?startDate=...&endDate=...`
- Report schedules:
  - `GET /api/report-schedules`
  - `GET /api/report-schedules/{id}`
  - `GET /api/report-schedules/schedule-id/{scheduleId}`
  - `GET /api/report-schedules/active`
  - `GET /api/report-schedules/user/{userId}`
  - `POST /api/report-schedules`
  - `PUT /api/report-schedules/{id}`
  - `PUT /api/report-schedules/{id}/activate`
  - `PUT /api/report-schedules/{id}/deactivate`
  - `POST /api/report-schedules/{id}/execute`
  - `DELETE /api/report-schedules/{id}`
  - `GET /api/report-schedules/check-schedule-id?scheduleId=...`
- Notification inbox and actions:
  - `GET /api/notifications/user/{userId}`
  - `GET /api/notifications/user/{userId}/unread`
  - `GET /api/notifications/user/{userId}/unread-count`
  - `GET /api/notifications/{id}`
  - `PUT /api/notifications/{id}/read`
  - `PUT /api/notifications/user/{userId}/read-all`
  - `DELETE /api/notifications/{id}`
  - `GET /api/notifications/stats?period=...`
  - `POST /api/notifications/retry-failed`
- Notification send or schedule:
  - `POST /api/notifications/send`
  - `POST /api/notifications/schedule`
- Templates:
  - `GET /api/notification-templates`
  - `GET /api/notification-templates/{id}`
  - `GET /api/notification-templates/code/{templateCode}`
  - `GET /api/notification-templates/type/{type}`
  - `GET /api/notification-templates/channel/{channel}`
  - `POST /api/notification-templates`
  - `PUT /api/notification-templates/{id}`
  - `PUT /api/notification-templates/{id}/activate`
  - `PUT /api/notification-templates/{id}/deactivate`
  - `DELETE /api/notification-templates/{id}`
  - `POST /api/notification-templates/{templateCode}/render`
  - `GET /api/notification-templates/check-code?templateCode=...`
