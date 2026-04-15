# Auth and Identity UX Design

## Outcomes

- allow user to sign in with correct org context
- support onboarding-first signup
- let multi-org users switch organization cleanly
- expose self-service profile management
- give admins a usable employee membership management area

## Primary Users

- public user during signup
- owner
- admin
- employee
- platform admin

## Information Architecture

- public
  - login
  - signup
- authenticated utility area
  - profile
  - organization switcher
  - logout
- admin area
  - employees
  - roles reference usage inside employee forms

## Screen Inventory

### Login

Purpose:
- authenticate and decide next route

Inputs:
- username or email
- password

Secondary actions:
- signup
- forgot password later

Route rules after success:
- `onboardingRequired = true` -> org onboarding
- one active membership -> direct dashboard
- multiple memberships -> org chooser if needed

### Signup

Purpose:
- create account-only identity

Inputs:
- username
- email
- phone optional
- first name
- last name
- password

Important note:
- signup success should not imply a completed ERP account

### Organization Chooser

Purpose:
- choose active org for users with multiple memberships

Card contents:
- org name
- org code
- role in org
- active status

Actions:
- continue into org

### Profile

Tabs:
- personal info
- security
- memberships
- active organization

### Employee Directory

Purpose:
- manage memberships within current organization

### Employee Create or Edit

Purpose:
- create org membership for a person-account
- assign role and branch access

## List Page Blueprint: Employee Directory

Columns:
- employee code
- display name
- username
- role
- default branch
- active status
- created at

Filters:
- role
- branch
- status
- search

Top actions:
- add employee
- export later

Row actions:
- view
- edit
- activate or deactivate

## Detail Page Blueprint: Employee Detail

Sections:
- identity summary
- contact summary
- role and permissions summary
- assigned branches
- audit details later

Inline actions:
- edit
- activate or deactivate

## Create or Edit Flow Blueprint

Step groups:

1. identity
- full name
- username
- email
- phone

2. role
- role picker
- employee code

3. branch access
- multi-select branches
- default branch

4. status
- active toggle

UX rules:
- default branch must be inside selected branches
- role picker should be searchable

## State Handling

Login:
- invalid credentials
- locked account
- expired access token with refresh fallback

Signup:
- username already exists
- email already exists
- onboarding success state

Employee list:
- no employees yet
- no results for filters

Employee form:
- duplicate employee code
- invalid default branch selection

## CTA Map

Login page:
- primary: Login
- secondary: Create account

Signup page:
- primary: Create account
- secondary: Back to login

Employee list:
- primary: Add employee
- row CTA: View
- secondary row CTA: Edit

Employee detail:
- primary: Edit employee
- secondary: Activate or deactivate

## Desktop and Mobile Notes

Desktop:
- employee list should use table layout
- profile page can use left nav tabs

Mobile:
- use stacked cards for memberships and employees
- organization switcher should be full-screen sheet
- employee branch assignment should use searchable modal picker

## Cross-Module Navigation

- after onboarding org creation, call org switch and route into setup
- profile should link to current org and memberships
- employee detail should deep-link to branch detail

## Key Forms And Backend Fields

### Login

- `username`
- `password`
- `organizationId`: optional
- `clientType`: optional override, otherwise backend derives from headers
- request headers worth wiring on app clients: `User-Agent`, `X-Device-Id`, `X-Device-Name`

### Signup

- `username`
- `email`
- `password`
- `firstName`
- `lastName`
- `phone`

### Organization Switch

- `organizationId`

### Employee Create

- `organizationId`
- `username`
- `password`
- `fullName`
- `email`
- `phone`
- `roleCode`
- `employeeCode`
- `defaultBranchId`
- `branchIds`
- `active`

### Employee Update

- `fullName`
- `email`
- `phone`
- `roleCode`
- `employeeCode`
- `defaultBranchId`
- `branchIds`
- `active`

## Screen To API Map

- Login:
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
- Signup:
  - `POST /api/auth/register`
- Organization switcher:
  - `POST /api/auth/switch-organization`
- Password change:
  - `POST /api/auth/change-password?userId=&oldPassword=&newPassword=`
- Employee list:
  - `GET /api/erp/employees?organizationId=...`
- Employee detail:
  - `GET /api/erp/employees/{id}?organizationId=...`
- Role picker:
  - `GET /api/erp/employees/roles?query=...`
- Employee create:
  - `POST /api/erp/employees`
- Employee update:
  - `PUT /api/erp/employees/{id}?organizationId=...`
- Employee activate or deactivate:
  - `PUT /api/erp/employees/{id}/activate?organizationId=...`
  - `PUT /api/erp/employees/{id}/deactivate?organizationId=...`
