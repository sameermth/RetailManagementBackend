# Release Branch Protection & PR Workflow

## Architecture Overview

```
main (production)
  ↑
  │ (merge from release with tags)
  │
release/1.0, release/1.1, etc
  ↑
  │ (only via PR from develop, creates tags & docker images)
  │
develop (staging/integration)
  ↑
  │ (feature branches, PRs from developers)
  │
feature/*, bugfix/*, etc
```

## Workflow Steps

### Step 1: Development
- Work on `develop` branch or feature branches
- Push to `develop` triggers: **Build & Test**
- No Docker image is created

### Step 2: Create Release Branch
```bash
# On develop branch
git checkout -b release/1.0
git push -u origin release/1.0
```

### Step 3: Merge Develop → Release (via PR)
1. Create Pull Request from `develop` → `release/1.0`
2. GitHub Actions automatically runs tests
3. Once approved, merge the PR
4. This **ONLY** way to update release branch (direct push is blocked)

### Step 4: Release Workflow Triggers Automatically
After PR merge to `release/1.0`:

1. ✅ **Build with Gradle**
2. ✅ **Run Tests**
3. ✅ **Create Git Tag**: `1.0`
4. ✅ **Build Docker Image**: `ghcr.io/sameermth/retailmanagementbackend:1.0.123`
   - Version format: `{release-version}.{github-run-number}`
   - Example: `1.0.1`, `1.0.2`, `1.1.1`, etc.
5. ✅ **Push to GitHub Container Registry**

### Step 5: Optional - Merge to Main
```bash
git checkout main
git pull origin main
git merge release/1.0
git push origin main
```

## Branch Protection Rules (To Configure in GitHub)

### For `release/*` branches:
- ✅ **Require a pull request before merging**
  - Require 1 approval
  - Dismiss stale reviews on new commits
  
- ✅ **Require status checks to pass**
  - `build-test-and-deploy` workflow must pass
  
- ✅ **Restrict who can push**
  - Block direct pushes (only admins/maintainers can bypass)
  
- ✅ **Prevent force pushes**
  - Cannot rewrite history
  
- ✅ **Prevent deletion**
  - Branches cannot be accidentally deleted

## Example Tags & Docker Images

| Release Branch | Git Tag | Docker Image Tag |
|---|---|---|
| release/1.0 | 1.0 | 1.0.123 |
| release/1.0 | 1.0 | 1.0.124 (after 2nd push) |
| release/1.1 | 1.1 | 1.1.45 |
| release/2.0 | 2.0 | 2.0.89 |

## Important Notes

- **GitHub run number** increments with each workflow run, ensuring unique image versions
- **Direct pushes to release branches are blocked** - all updates must come via PR
- **Force push is disabled** - prevents accidental history changes
- **Branch deletion is disabled** - prevents accidental removal
- **Status checks required** - release branch can only be updated if tests pass

