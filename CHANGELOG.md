# Changelog

## [Unreleased]

### Added

  - Edge page. Nuvlabox details still hidden wip

### Changed

  - Deployment page - all urls are visible in deployment details 
    and module link is present in details
  - Deployment modal - support env variables
  - Apps page - add env variables to module component
  - Deployment owners replaced by metadata widget
  - Metadata widget - doesn't show acl when acl is null
  - ACL widget - indeterminate state in simple mode
  - Dashboard page - URL in deployment card url is only visible when 
    deployment state is started. Rename namespace to dashboard
  - Redirect to welcome page when token is root
  - Fix redirection behavior when loading new tab
  - CMD + click on history link open in new tab
  - Add job action to deployment details and limit jobs number to 10
  - Align with API-SERVER field type renamed subtype 
    (not backward compatible)

## [2.0.2] - 2019-05-22

### Changed

  - Release script fix
  - Deployment - force refresh deployments on delete
  - Message bootstrap - hide on logout
  - Collection templates - force refresh on session change
  - Authn Signup/Create user visibility depend on ACLs
  - Footer - Fix code version in footer
  - Authn - Fix signup and cleaner validatioi code
  - Page Deployment - Make job message multiline
  - Page api - Document button not activated on return
  - Page api - Refresh results on delete or add resource
  - Message bootstrap - check triggered if session not nil

## [2.0.1] - 2019-05-22 - broken tag

## [2.0.0] - 2019-05-20

### Changed

  - Add ACL button to apps pages
  - Better session expiry behavior
  - Update version of clojure API to 2.0.0
  - Update parent to version 6.5.0 and shadow-cljs
  
### Added

   - Infrastructure page
   - Credential page
   - ACL button with rights summary as icon
   - New ACL widget

## [0.0.1] - 2019-04-18

### Changed

  - Update parent to version 6.3.0.
  - Test release process.
 
