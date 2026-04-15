# Branch Protection Setup Guide

This document explains how to set up branch protection rules for the release branches.

## Release Branch Strategy

- **Release Branch Pattern**: `release/*` (e.g., `release/1.0`, `release/1.1`, `release/2.0`)
- **Updates**: Only via Pull Requests from `develop` branch
- **Direct Pushes**: Blocked
- **Requirement**: All PRs must be reviewed and approved before merging

## Setting Up Branch Protection Rules on GitHub

Follow these steps to configure branch protection:

### 1. Go to Repository Settings
- Navigate to your GitHub repository
- Click **Settings** (top menu)
- Click **Branches** (left sidebar)

### 2. Add Branch Protection Rule
- Click **Add rule**
- Pattern: `release/*`
- Click **Create**

### 3. Configure Protection Rules

Under the `release/*` rule, enable the following:

#### Basic Settings
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: `1`
  - ✅ Require review from code owners (if applicable)
  - ✅ Dismiss stale pull request approvals when new commits are pushed

#### Advanced Settings
- ✅ **Require status checks to pass before merging**
  - ✅ Search and select: `build-test-and-deploy` (GitHub Actions workflow)
  
- ✅ **Restrict who can push to matching branches**
  - Select: Specify users or teams who can bypass these rules (usually Admins only)

- ✅ **Allow force pushes**: ❌ Disabled
- ✅ **Allow deletions**: ❌ Disabled (prevent accidental deletion)
- ✅ **Require branches to be up to date before merging**: ✅ Enabled

### 4. Save Changes
- Click **Save changes** at the bottom

## Workflow for Release

1. **Development on `develop` branch**
   - All feature development happens on `develop`
   - Run tests and build using develop workflow

2. **Create Release Branch**
   - When ready for release, create a new branch from `develop`: `release/1.0`
   - `git checkout develop && git checkout -b release/1.0 && git push -u origin release/1.0`

3. **Create Pull Request**
   - Open a PR from `develop` to `release/1.0`
   - This triggers the release workflow
   - Includes build, test, and Docker image push to GHCR with version tags

4. **Merge to Release**
   - After approval and status checks pass, merge the PR
   - This automatically builds and pushes the Docker image with version: `1.0.{build-number}`
   - Creates a Git tag: `1.0`

## CI/CD Workflow Overview

### Develop Branch Workflow
- **Trigger**: Push to `develop`
- **Steps**: Build → Test
- **Outcome**: Validates code quality

### Release Branch Workflow
- **Trigger**: Push to `release/*` (via PR merge only)
- **Steps**: 
  1. Build
  2. Run tests
  3. Create Git tag (e.g., `1.0`)
  4. Build and push Docker image to GHCR with tag: `{version}.{build-number}` (e.g., `1.0.123`)
- **Outcome**: Release-ready Docker image in GitHub Container Registry

## Important Notes

- **No Direct Pushes**: `git push` directly to `release/*` branches will be rejected
- **Force Push Disabled**: Prevents accidental history rewriting
- **Deletion Prevention**: Branches cannot be accidentally deleted
- **Automatic Tags**: Git tags are created automatically during workflow execution

