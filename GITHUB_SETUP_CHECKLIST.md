# GitHub Setup Checklist for Branch Protection

## ✅ Quick Setup Guide

Follow these steps to enable branch protection on GitHub:

### 1. Navigate to Repository Settings
- [ ] Go to your repository on GitHub
- [ ] Click **Settings** (top right menu)
- [ ] Click **Branches** (left sidebar)

### 2. Add Branch Protection Rule for `release/*`
- [ ] Click **Add rule**
- [ ] Enter pattern: `release/*`
- [ ] Click **Create**

### 3. Configure Protection Settings
Check the following options:

#### 🔒 Access Control
- [ ] ✅ **Require a pull request before merging**
  - [ ] Set required approvals: **1**
  - [ ] ✅ Require review from code owners (if you have a CODEOWNERS file)
  - [ ] ✅ Dismiss stale pull request approvals when new commits are pushed

#### 🧪 Quality Checks
- [ ] ✅ **Require status checks to pass before merging**
  - [ ] Search for and select: **`build-test-and-deploy`** (GitHub Actions workflow)
  - [ ] ✅ Require branches to be up to date before merging

#### 🛡️ Safety
- [ ] ✅ **Restrict who can push to matching branches**
  - [ ] Select: **Specify who can bypass** (usually just admins)
- [ ] ✅ **Allow force pushes**: **Disabled** (Do NOT check)
- [ ] ✅ **Allow deletions**: **Disabled** (Do NOT check)

### 4. Save Changes
- [ ] Scroll to bottom and click **Save changes**

## ✅ Workflow Files Status
- [x] `.github/workflows/develop.yml` - Build & Test on develop branch
- [x] `.github/workflows/release.yml` - Build, Test, Tag & Docker push on release branch
- [x] `.gitignore` - Updated to include gradlew
- [x] `gradlew` and `gradlew.bat` - Committed to repository
- [x] JDK 21 - Configured in both workflows

## ✅ Documentation Files
- [x] `BRANCH_PROTECTION_SETUP.md` - Detailed setup instructions
- [x] `RELEASE_WORKFLOW_GUIDE.md` - Full workflow architecture and examples
- [x] `GITHUB_SETUP_CHECKLIST.md` - This checklist

## ✅ Release Process Steps

1. **Development**: Push to `develop` → Triggers Build & Test workflow
2. **Create Release Branch**: `git checkout -b release/1.0 && git push -u origin release/1.0`
3. **Create PR**: Open PR from `develop` → `release/1.0`
4. **Review & Merge**: After approval, merge the PR
5. **Auto Deploy**: Release workflow triggers automatically:
   - Builds & tests code
   - Creates git tag: `1.0`
   - Pushes Docker image: `ghcr.io/sameermth/retailmanagementbackend:1.0.{run-number}`

## ⚠️ Important Notes

- **Direct pushes to release branches are blocked** - Use PR from develop only
- **Docker image tag format**: `{version}.{github-run-number}` (e.g., `1.0.123`)
- **Git tag format**: `{version}` (e.g., `1.0`)
- **Multiple runs** on same branch create new images: `1.0.123`, `1.0.124`, etc.
- **Tag existence check** prevents errors if tag already exists

## 🔗 Useful Commands

```bash
# List all local and remote branches
git branch -a

# List all tags
git tag -l

# Create and push release branch
git checkout develop
git pull origin develop
git checkout -b release/1.0
git push -u origin release/1.0

# Pull latest updates
git fetch --prune
```

## 📚 Documentation Files Location
- `BRANCH_PROTECTION_SETUP.md` - Step-by-step GitHub setup
- `RELEASE_WORKFLOW_GUIDE.md` - Full workflow architecture
- `.github/workflows/develop.yml` - Develop branch CI/CD
- `.github/workflows/release.yml` - Release branch CI/CD with Docker

