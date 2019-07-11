# Changelog

## [Unreleased]

## [2.3.0] - 2019-07-11

### Changed

  - App Component - Server conflict now on same path, notify user to 
    choose another name #108
  - App Component - Unclear how to fill docker image fields #96 #118 
    #199
  - Deployment detail - add a section for "Environment Variables" #183
  - Deployment - Harmonize cards between deployment details 
    and deployment page #185
  - Deployment detail - add a section for "URLs" #184
  - Deployment detail - clicking on an event or job link shows 
    spinner (forever) #179 
  - App Component - fix  Validation error remains when deleting a 
    component #196
  - App Component - empty env values are allowed #186
  - Apps - old search is applied even if search field is empty
  - Upgrade to parent 6.5.1, nuvla api 2.0.1, shadow-cljs 2.8.39 
  - App component - Creating a new component sees previously used env 
    vars #175
  - ACL Button - ACL Button hide itself when no acl and in read-only 
    mode #173
  - Apps component - architectures should be separated in read-only 
    mode #174
  - Infrastructure page - add on service group allows creation of more 
    than one service of the same type fix
  - Login - disable login for password and api-key when 
    not all required fields are complete
  - Infrastructure page - take into account acl at creation
  - Credential page - take into account acl at creation
  - ACL - refactor to get ui-acl format to be able to keep order
  - About page - Links update

## [2.2.0] - 2019-06-20

### Added

  - Set document title when navigating to simplify history navigation
  - Main components - Refresh Menu is now reusable and generalize it to 
    all pages
  - Action intervals - countdown feature and adding it on all 
    automatically refreshed pages 

### Changed

  - Module component - Architecture field changed to an array on server
  - Docs - loading animation on segment
  - Deployment - credential-id renamed parent
  - Deployment parameter - field deployment/href renamed parent
  - Dashboard refresh interval is set to 10s
  - Authn - login button and form on key enter same behavior 
  - Authn - make validation in signup and reset password less eager
  - Authn modals - disable autocomplete on singup, reset-password modals
  - Action intervals - moved to reframe db and refactored
  - Dasboard details - Add ACL button
  - Infrastructure - Add ACL button to crud modals
  - Credentials - Add ACL button to crud modals
  - Acl - Enhance acl button
  - ACLs - Automatically add new rights when user select a principal 
    and a right in new permissions row
  - Apps - Project save should not show commit message 
    and cancelling a commit message when saving a component 
    should not remove is-new? flag
  - Apps - remove save button on top bar
  - Edge - refresh set to 10 seconds and remove floating time tolerance
  - Edge detail - re-use status from edge page
  - Dashboard detail - issue with refreshing deployment output parameter

## [2.1.1] - 2019-06-12

### Changed

  - reuse action-button and delete duplicated code
  - fix broken link in welcome page
  - deployment card - fix regression in deployment card
  - app component creation - fix app component ports headers 
  - invite user - rename create user to invite user
  - fn can-delete? fix a bug
  - cimi api effects - delete support by default on-error
  - Edge details - delete operation added

## [2.1.0] - 2019-06-11

### Added

  - Edge page. Nuvlabox details

### Changed

  - Deployment details - jobs section are paginated
  - Deployment page - ame eployment card is visible in details
  - ACLs - Fix bug in indeterminate state
  - Avoid as much as possible blank page at initialization
  - Move client out of DB
  - Apps - saving a project do not interrupt for a commit message
  - Infrastructures - edit depend now on credentials acls
  - Credentials - displayed actions depend now on credentials acls
  - Signup - Validation of an email show a signup success message
  - Signup - Submit button is disabled and form cleared after a 
    successful submit
  - Create user - email of invited user is prefilled
  - Session and User templates based modals display clear validation 
    and human readable errors 
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
 
