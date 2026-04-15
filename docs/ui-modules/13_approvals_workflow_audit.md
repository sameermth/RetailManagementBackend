# Approvals, Workflow, and Audit UX Design

## Outcomes

- give approvers a fast decision queue rather than a passive log screen
- make request context understandable before approve or reject actions
- separate approval execution from audit review so the UI stays clear
- help owners and admins trace what happened, by whom, and when

## Primary Users

- owner
- admin
- approver
- finance approver
- platform admin in oversight mode

## Information Architecture

Primary navigation under `Controls` or `Operations`:
- approvals
- workflow monitor
- audit log

Recommended relationships:
- approval queue is action-oriented
- workflow monitor is system-oriented
- audit log is traceability-oriented

## Screen Inventory

### Approval Queue

Purpose:
- primary action queue for pending decisions

### Approval Detail

Purpose:
- decision workspace with enough source context to approve or reject confidently

### Approval Rule Manager

Purpose:
- maintain who must approve what

### Workflow Trigger Monitor

Purpose:
- inspect backend-generated workflow triggers or suggestions

### Audit Log

Purpose:
- searchable trace of major changes and actions

### Audit Detail or Side Panel

Purpose:
- expand a single event without leaving the current list unnecessarily

## List Page Blueprint

### Approval Queue

Columns:
- approval request id
- entity type
- entity reference
- requester
- branch
- created at
- status
- priority later if available

Filters:
- entity type
- branch
- requester
- pending only
- date range
- search by reference or requester

Top actions:
- open next pending

Row actions:
- view
- approve
- reject
- cancel if allowed

### Approval Rule Manager

Columns:
- rule name
- entity type
- approver role
- threshold or condition summary
- active

Filters:
- entity type
- active
- search by rule name

Top actions:
- create rule

Row actions:
- view
- edit
- activate or deactivate

### Workflow Trigger Monitor

Columns:
- trigger type
- entity type
- entity reference
- reason summary
- dispatch status
- created at

Filters:
- trigger type
- dispatch status
- entity type
- date range

### Audit Log

Columns:
- occurred at
- entity type
- entity reference
- action
- actor
- summary

Filters:
- entity type
- action
- actor
- date range
- search by entity reference

## Detail Page Blueprint

### Approval Detail

Header summary:
- request id
- entity type
- entity reference
- requester
- current status

Key sections:
- request summary
- impacted values or payload snapshot
- supporting notes
- approval chain or history

Decision area should show:
- approve CTA
- reject CTA
- rejection reason input

Important UX rule:
- user should see enough context to decide without having to manually open five other screens
- but source-document deep links should still be available

### Workflow Trigger Detail

Header summary:
- trigger type
- entity
- status
- reason

Sections:
- generated reason
- downstream action taken
- linked source entity

### Audit Detail

Use a side panel or drawer with:
- timestamp
- actor
- action
- entity type and reference
- change summary
- raw change block later if needed

## Create or Edit Flow Blueprint

### Approval Rule Create

Sections:
- rule identity
- entity type
- condition or threshold
- approver role or path
- active state

If thresholds apply:
- use dedicated numeric section with clear units and currency

### Approval Decision Flow

Step 1 review request:
- summary
- source values

Step 2 review impact:
- what changes if approved or rejected

Step 3 decision:
- approve
- reject with reason

### Workflow Monitor Review Flow

This is less of a create screen and more of an operational review:
- filter triggers
- open detail
- inspect reason and dispatch state

## State Handling

Approvals:
- pending
- approved
- rejected
- cancelled

Rules:
- active
- inactive
- no rules configured

Workflow monitor:
- pending dispatch
- dispatched
- failed
- ignored if supported later

Audit:
- empty result state
- heavy result state with pagination

Permission handling:
- approvers can act
- read-only users can inspect but not decide
- audit is usually read-only, but restricted by role

## CTA Map

Approval queue:
- primary: Open next pending
- row primary: View
- row secondary: Approve or Reject

Approval detail:
- primary: Approve
- secondary: Reject

Rule manager:
- primary: Create rule
- row primary: View
- row secondary: Edit

Audit log:
- row primary: View detail

## Desktop and Mobile Notes

Desktop:
- approval detail should keep summary and action area visible together
- audit works well with table plus side drawer pattern

Mobile:
- approval review is possible, but dense payload comparisons are better on tablet or desktop
- use compact action bar for approve and reject only where policy allows

## Cross-Module Navigation

- approval detail to purchase, sales, finance, or expense source entity
- workflow trigger to source document
- audit event to entity detail page
- approval rule to affected module settings later

## Key Forms And Backend Fields

### Approval Rule Create

- `entityType`
- `approvalType`
- `minAmount`
- `maxAmount`
- `approverRoleId`
- `priorityOrder`
- `active`

### Approval Request Create

- `entityType`
- `entityId`
- `entityNumber`
- `approvalType`
- `requestReason`
- `currentApproverUserId`
- `currentApproverRoleSnapshot`

### Approval Action

- `remarks`

### Approval Evaluation

- `entityType`
- `entityId`
- `approvalType`

## Screen To API Map

- Rule list and create:
  - `GET /api/erp/approvals/rules?organizationId=...&entityType=...&branchId=...`
  - `POST /api/erp/approvals/rules?organizationId=...&branchId=...`
- Approval queue:
  - `GET /api/erp/approvals/requests?organizationId=...&status=...`
  - `GET /api/erp/approvals/requests/summary?organizationId=...`
- Approval detail:
  - `GET /api/erp/approvals/requests/{id}`
- Approval request create:
  - `POST /api/erp/approvals/requests?organizationId=...&branchId=...`
- Approval evaluate:
  - `POST /api/erp/approvals/evaluate?organizationId=...`
- Approval actions:
  - `POST /api/erp/approvals/requests/{id}/approve?organizationId=...`
  - `POST /api/erp/approvals/requests/{id}/reject?organizationId=...`
  - `POST /api/erp/approvals/requests/{id}/cancel?organizationId=...`
- Entity audit trail:
  - `GET /api/erp/audit-events?entityType=...&entityId=...`
