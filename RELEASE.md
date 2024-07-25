# Release Process

Release of deployment repository is managed by [Release Please](https://github.com/google-github-actions/release-please-action).
This tool allow to:
1. Automate CHANGELOG generation
2. The creation of GitHub releases
3. Version bumps for the project

It does so by parsing the git history, looking for Conventional Commit messages, and creating release PRs.

On push to master, Release please tool will create a PR that shows to you next version and changelog update.

To make a release, the maintainer should merge the release PR.

After the merge of the releaese PR, Release please will create another PR with snapshoted version. 
Maintainer should merge it when the release is done successfully.
