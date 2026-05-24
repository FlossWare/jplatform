# CI/CD Configuration

This directory contains scripts and documentation for the JPlatform CI/CD pipeline.

## Versioning Strategy

JPlatform uses **X.Y semantic versioning** (e.g., 1.1, 1.2, 1.3):
- **X**: Major version (manual increments for breaking changes)
- **Y**: Minor version (auto-incremented on every commit to main)

## Automated Release Process

On every push to the `main` branch, GitHub Actions automatically:

1. **Increments** the minor version (1.1 → 1.2)
2. **Builds** all modules with `mvn clean install`
3. **Tests** all modules
4. **Deploys** artifacts to packagecloud.io
5. **Commits** the version bump with `[ci skip]`
6. **Tags** the release with `vX.Y`

## GitHub Secrets Required

The following secrets must be configured in GitHub repository settings:

- `PACKAGECLOUD_TOKEN`: Bearer token for packagecloud.io API authentication
- `GITHUB_TOKEN`: Automatically provided by GitHub Actions

## Manual Version Bump

To manually increment the version locally:

```bash
./ci/rev-version.sh
```

This script:
- Reads current version from pom.xml
- Increments minor version
- Updates all module versions
- Commits and tags
- Pushes to GitHub

## Distribution

Artifacts are published to:
- **Repository**: https://packagecloud.io/flossware/java/maven2/
- **GroupId**: org.flossware.jplatform
- **ArtifactId**: Various (jplatform-api, jplatform-core, etc.)
- **Version**: X.Y format

## Maven Configuration

Users can consume published artifacts by adding to their `pom.xml`:

```xml
<repositories>
    <repository>
        <id>packagecloud-flossware</id>
        <url>https://packagecloud.io/flossware/java/maven2</url>
    </repository>
</repositories>

<dependency>
    <groupId>org.flossware.jplatform</groupId>
    <artifactId>jplatform-api</artifactId>
    <version>1.2</version>
</dependency>
```

## Version Enforcement

The Maven Enforcer Plugin ensures:
- ✅ Version format is exactly X.Y (no SNAPSHOT, no X.Y.Z)
- ✅ All dependencies are release versions
- ✅ Build fails if version format is incorrect

## Preventing Infinite CI Loops

The workflow includes:
```yaml
if: github.event.pusher.email != 'version-bump@flossware.org'
```

This prevents version bump commits from triggering another CI run.

## Workflow File

See `.github/workflows/main.yml` for the complete CI/CD configuration.
