# Changelog

## Unreleased

### Changed

  - Docs - Fetch documents when user goes directly to details page
  - Profile - Send events to Intercom related to trial period
    end email notifications
  - Deployment modal - On update credential can be changed
  - Deployment modal - On update only selected infra is visible
  - Tools - Re-frame-10x is disabled by default

## [2.12.0] - 2020-12-10

### Changed

  - Edge - fix SSH key assignment to NuvlaBox at creation time
  - Credential views - Fix SSH visibility in credentials list
  - Filter comp - Processing of resources metadata do
    not consider vector of map type fix (#499)
  - Edge detail - minor fixes

## [2.11.0] - 2020-12-07

### Added

  - BUILD - Support for github actions

### Changed

  - Deployment detail - Support udpate feature
  - Deployment modal - Support udpate feature
  - Deployment modal - Add version selection section
  - Edge Detail - new tab based design

## [2.10.3] - 2020-11-17

### Changed

  - CIMI DETAIL - operation button action regression fix

## [2.10.2] - 2020-11-16

### Changed

  - CIMI DETAIL - cimi resources presented as keys fix

## [2.10.1] - 2020-11-16

### Changed

  - Edge - Remove filter prototype

## [2.10.0] - 2020-11-16

### Added

  - Filter composer component
  - Deployment - Info message about VPN when the infra has a private IP

### Changed

  - Cimi page - Filter composer
  - Deployment - Stop modal fix checkbox danger modal
  - Profile - Billing contact
  - Resource-metadata - Too big, optimization to get it only
    on api documentation page
  - Sidebar - logo link is defined by config nuvla-logo-url
  - Config - new nuvla-logo-url attribute
  - Config - new pricing-url attribute
  - Pricing - Remove pricing page
  - Deployment - Click on error message make user
    go to job section
  - NuvlaBox - add new icon for Bluetooth peripherals

## [2.9.0] - 2020-10-28

### Changed

  - Deployment - Show last failed job on top
  - Dashboard detail - renamed deployment
  - Deps - Popper dependency fix
  - Dashboard detail - version bug fix
  - Shutdown modal - check credential
  - Sign-up - Redirect to sign-in after sign-up to fix
    safari save password
  - Apps - Docker image registry placeholder fix
  - Apps - Version not taken into account at launch fix
  - Apps - License Terms & Conditions url updated
  - Main - Nuvla logo redirect to /

## [2.8.1] - 2020-10-15

### Changed

  - Edge - Optimize number of requests for stats
  - Use sticky bar for mostly all menubar in pages
  - Main components - Sticky bar
  - Dependencies update
  - Improved launch dialog steps validation

## [2.8.0] - 2020-10-09

### Added

  - Deployment modal - Price and license sections
  - Deployment detail - New billing section
  - Apps - Show price when nonfree module
  - Module - Price and license sections for module
  - Profile - Vendor section
  - Deployment modal - Show if docker images are trusted on the summary
  - Nuvlabox detail - Extended view for peripheral list in NuvlaBox details
    page, to cope with new resource telemetry attributes

### Changed

  - Pricing - Set VPN price as included
  - Credential - Remove check for subscription on creation of VPN or IaaS
  - Deployment modal - Sections are now vertical

## [2.7.0] - 2020-09-04

### Changed

  - User dropdown - z-index fix
  - Subscription modal - group mandatory email field
  - Remove duplicate subscription active-claim
  - Dependencies update
  - Subscription status past due considered as active
  - Dependency - Update semantic-ui-react to v1.1.0

## [2.6.0] - 2020-07-31

### Added

  - Re-usable button layout for extra actions menus
  - Ability to rename NuvlaBox resource

### Changed

  - Credential - Add VPN credential not possible from group warning
  - Added support for file signature with GPG for
    nuvlabox-self-registration.py
  - Switch group adjustment
  - Update deps

## [2.5.0] - 2020-07-06

### Added

  - New credential creation workflow for SSH keys
  - SSH key association for NuvlaBox at creation time
  - SSH key listing in NuvlaBox details page
  - Awesome fonts
  - Profile page - Customer and stripe integration
  - Pricing page
  - Stripe components
  - Credentials for cloud providers AWS, Azure, Google, Exoscale.
  - Infrastructure service modal augmented with creation of
    Docker Swarm or Kubernetes on cloud providers.

### Changed

  - Apps - Edit of ports mapping, urls, env vars, output params,
    volumes, files and data-types inserted randomly when more
    than 8 values fix
  - Dashboard - fix show empty URL in table view
  - Config - new attribute stripe
  - Sign-up - Integration with pricing feature
  - Main - Modal subscription required
  - NuvlaBox detail - Fix bug when saving location
  - Danger-Modal - Fix shutdown deployment in some condition button
    had no effect
  - Fix typos and text consistency
  - Add instructions on how to enable fast deployment monitoring

## [2.4.15] - 2020-05-12

### Changed

  - Container - exclude config.json and nuvla-logo.png from
    precompress mvn prepare-package

## [2.4.14] - 2020-05-11

### Added

  - Dashboard detail - deplyment in stopped state are restartable
    and rename stop operation shutdown in UI
  - Config file - Load config file to set UI side configuration
  - Intercom Component
  - Auth menu - Support act as a group via claim action
  - downloads directory to host downloadable Nuvla assets
  - nuvlabox-self-registration download script, for the NuvlaBox industrialization
  - OS selection step in NuvlaBox creation workflow
  - Generation of API key and USB trigger file for auto-installing NuvlaBox

### Changed

  - Dashboard detail - Show module versions
  - Dashboard detail - Fetch clone workflows
  - Added new peripheral attributes to Edge detail view's accordion
  - Files are ignored for docker-compose applications
  - Fixed NuvlaBox version selection bug about picking up wrong modules
  - Translate all NuvlaBox modal content to French

## [2.4.13] - 2020-04-14

### Changed

  - Welcome page - Update content
  - Credentials - i18n
  - Momentjs - require locale fr
  - UIX comp - use setInterval behave better with dev reloading
  - Action Interval - use setInterval behave better with dev reloading
  - NuvlaBox Edge detail - Show peripheral action in accordion title
  - NuvlaBox Edge detail - New plots and stats

## [2.4.12] - 2020-03-27

### Added

  - Count down component
  - Re-frame-10x - inspection and debug tools
  - New Dependency - add jszip library

### Changed

  - Action interval - Optimization
  - Application - Module compatibility and docker-compose validation
    on get module
  - Dependencies - udpate parent to v6.7.3
  - Edge page – Create nuvlabox modal generate a single zip for NuvlaBox
    engine
  - New Dependency - jszip add library
  - Update dependencies to latest react/reagent/re-frame and others
  - Popup credential connectivity check disappear fix
  - Data workflow - module of subtype project should not appear
  - Deployment modal - Align deployment data filter spec with api-server
  - Profile - change password modal fix
  - Retry get cloud-entry-point first load and keep loading

## [2.4.11] - 2020-03-06

### Added

 - Egde - Automatic NuvlaBox release selection and peripheral correlation
 - Edge - Dynamic compose file generation upon NuvlaBox creation
 - Edge - Installation guides upon NuvlaBox creation
 - App view - compatibility label
 - Edge - Action buttons to peripherals

### Changed

 - Data page / Deployment modal - replace data-records map by
   data-records-filter
 - Component - New component time-ago
 - Deployment modal - check credential of registries
 - Module component - fix bug in edit with private registries
 - DEPENDENCIES - update dependencies
 - Deployment modal - support private registries
 - Module component -  Support private registries
 - Module app - Support private registries
 - Credentials - Docker private registries
 - Infrastructure - Support Docker private registries
 - Infrastructure - view infra status and if swarm mode is enabled
 - Credentials - Bring back S3
 - Infrastructures - Bring back S3
 - Edge detail - replace horizontal bars by gauge charts
 - OCRE - updated voucher schema and CSV file header validation

## [2.4.10] - 2020-02-07

### Changed

  - Edge details - Display tags in NuvlaBox cards
  - Infrastructure - User able to view should be able to go to details page
  - Edge details - add update notification message on edit
  - Edge details - longitude have to be normalized before update in ES
  - Edge details - remove pagination from map view
  - Edge details - confirm new location with buttons
  - Edge details - position in leaflet it's [lat long], in ES it's [long lat] fix
  - Infra page - Fix blank page when not known subtype
  - Authn menu - Fix support text and set user when logged in
  - Edge details - allow add and edit of location on Map
  - Edge - NuvlaBox displayed on map
  - Map leaflet integration
  - Search input - Full text search value is lost on navigation but the filter
    stay fix
  - Credential page - fix regression in showing credential modal

## [2.4.9] - 2020-01-23

### Changed

  - Session - add getting started link and terms and conditions link
  - Replace modal delete in apps, deployment, edge, infrastructure, credentials
  - COMPONENT - Modal danger with warning and checkbox confirmation
  - CIMI PAGE - Sort from table columns header
  - Deployment modal - Add credential check
  - Deployment modal - Filter infrastructure services by module subtype
  - Deployment modal - Credential step is replaced by infrastructure services step
  - Session page should not appear when reloading the page
  - DASHBOARD DETAIL PAGE - ACL widget disapearing sometimes on refresh fix
  - EDGE PAGE - Let user choose cards or table display for NuvlaBoxes
  - INFRASTRUCTURE PAGE - Ignore changes modal is visible even when we save fix
  - Deployment modal - Steps keeps at top when scrolling
  - APPS PAGE - When user is not logged in, launch button should not be visible
  - APPS PAGE - When user is not logged in, set-module is getting
    cloud-entry-point instead of nil fix

## [2.4.8] - 2020-01-10

### Changed

  - SESSION PAGE - Use proper page for authentication instead of modals
  - DEPENDENCIES - Fix version of closure-compiler-unshaded to be
    aligned with shadow-cljs version
  - DEPENDENCIES - update dependencies
  - DEPENDENCIES - New validation utility for form fields
  - INFRASTRUCTURE PAGE - Remove S3 from add modal
  - CREDENTIAL PAGE - Remove S3 from add modal
  - DASHBOARD - card <a> can't be descendant of <a>
  - Update copyright footer to 2020
  - NUVLABOX DETAIL - Allow sharing NuvlaBox via ACL
  - OCRE - When importing, if voucher exist update it instead of conflict
  - OCRE - Make visible vouchers by default to all group users
  - OCRE - Server side aggregation for pie chart
  - OCRE - Set default query params at page enter to remove influence
    from CIMI page
  - CIMI PAGE - remove terms from statistic numbers since badly rendered
  - DASHBOARD DETAIL - Support Kubernetes logs
  - DASHBOARD DETAIL - Regression fix list of services for application not
    visible  

## [2.4.7] - 2019-12-09

### Changed

  - BOOTSTRAP_MESSAGE - Disable check for bootstarp message for swarm and
    creds
  - DASHBOARD PAGE - Clickable cards better visibility
  - DASHBOARD PAGE - Search Input
  - Main components - Search input
  - EDGE PAGE - Full text search
  - Pagination - make pagination visible even if only one page is available
  - EDGE PAGE - Replace dropdown filter by clickable icons
  - CREDENTIAL - add vpn modal, default description and name is set
    for the user. Warning is displayed for the user to request user
    to save his generated credential
  - CREDENTIAL - add modal select by default first infra if only
    one available
  - OCRE PAGE - add pie chart and align schema with api-server
  - CIMI PAGE - make name field visible by default
  - DEPENDENCIES - Fix firefox callback issue in delete caused by
    bluebird version 3.7.1
  - DASHBOARD DETAIL PAGE - re-order deployment sections
  - Devtools - install devtools in dev mode
  - EDGE DETAILS PAGE - Add openVPN support
  - CREDENTIAL PAGE - Add openVPN support
  - Dependencies - udpate parent and local dependencies

## [2.4.6] - 2019-11-13

### Changed

  - OCRE PAGE - Support bulk delete
  - CIMI PAGE - Support bulk delete
  - OCRE PAGE - ocre page added visible for admin and ocre users
  - CIMI page - rows are now clickable
  - App store - order cards by created time
  - Infrastructure page - fix inconsistency between pages
  - Application detail - validate yaml syntax and print
    and show error and hints messages
  - Dashboard detail - If not valid yaml blank page fix
  - Edge details - Support nuvlabox-peripherals resource
  - ACL - search users escape chars

## [2.4.5] - 2019-10-10

### Changed

  - Infrastructure - Allow multiple instances of same service subtype in infrastructure group

## [2.4.4] - 2019-09-18

### Changed

  - Dependencies - Update dependencies
  - Edge - Add modal version moved to an advanced section
  - Messages - Polling notifications enhance title and lifecycle management
  - Favicon added
  - Dashboard detail - fix concatenate log duplicates and rename scroll down to go live

## [2.4.3] - 2019-09-04

### Add

  - Dashboard detail - log accordion
  - Yaml parser dependency
  - Component - TableRowField

### Changed

  - Apps - Environmental variable align spec with server #230
  - Dashboard detail - remove transparency to make search dialog visible and minors
  - Apps store - Fix refresh
  - Apps - Fix summary fields on-change
  - Apps - Allow urls only when all replicas.running parameters are positive #227
  - Credentials, Infrastructure, App - Use TableRowField
  - Only show deployment URLs when number of running replicas is positive
  - Order output parameters alphabetically
  - Dependencies - Update shadow-cljs to version 2.8.45
  - Sidebar - Css scroll y set to auto to support paysage mode
  - Apps project - pageheader inline display
  - Codemirror - add search ability and highlight in scrollbar visible
  - Cimi detail - make coherent changes between raw and acl widget
  - Signup modal - submit on return key regression fix

## [2.4.2] - 2019-08-07

### Added

  - Authn - Support Github external authentication, API-SERVER version >=4.2.2 required to work
    properly
  - Welcome - Show error message coded in url sent by the server
  - SpanBlockJustified - Component to display long description cleanly

### Changed

  - Dependencies - Update shadow-cljs to version 2.8.42 and clojure-library to version 2.0.3
  - Footer - Visual changes made
  - Side bar - Visual changes made
  - Api page - Changed to be a non protected page
  - Side bar - Enhance detection of selected item #106 and click on protected pages open login
    page when no session #212
  - Login - Force refresh main content on set-session
  - Login - Login modal open automatically on protected pages
  - Refresh - Allow force refresh main content
  - Pages - Metadata for pages moved to db
  - Signup - Submit options differ between submit by enter or click fix
  - Dependencies - Update source-map-support to 0.5.13
  - ACL - Tooltip activation on hover #123
  - Edge - Statistics is now responsive
  - Shadow-cljs updated to version 2.8.42
  - Deployment modal - no files message not visible fix
  - Full text search should apply an and for spaces instead of or
  - Fix issue with IE11 Modal and footer
  - Update project dependencies
  - PageHeader - Breakline to allow very long name to be properly rendered
  - Apps - Component fix test command mounts misbehaviour
  - Apps - Summary is now a Textarea
  - Apps - Change logo and summary missbihaving responsive fix
  - Apps - Make plus button and trash cursor visible

## [2.4.1] - 2019-07-29

### Changed

  - Apps application - Remove fold-gutters from Docker-compose editor resolve visiblity issue
  - Reuse subs apps, deployment-modals, authn
  - Apps - Fix issue in controlled env variables
  - Deployment modal - fix bug in eddition of env varialbes
  - Page Header - Reuse page header

## [2.4.0] - 2019-07-24

### Changed

  - Deployment modal - deployment should not get mounts fields when no
    distinct mounts
  - Apps pages - Reuse editable as a subscription and remove duplicated
    code
  - Cimi detail - Fix ACL not visible and make it editable
  - Deployment modal - Ensure that a credential is set in deployment
    before start action

### Added

  - Application/Module - support deployement of application with
    docker-compose and files

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
