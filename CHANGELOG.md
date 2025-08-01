# Changelog

## Released

## [2.49.0](https://github.com/nuvla/ui/compare/2.48.0...2.49.0) (2025-06-30)


### Features

* **edge-docker-resources-v2:** Edge docker resources - phase 2 ([#1745](https://github.com/nuvla/ui/issues/1745)) ([582cd92](https://github.com/nuvla/ui/commit/582cd92efbf7f677bf12f8daa5577b28e2f41acf))

## [2.48.0](https://github.com/nuvla/ui/compare/2.47.1...2.48.0) (2025-06-30)


### Features

* **Deplyment details page:** New docker tab for NE that support COE resources ([#1750](https://github.com/nuvla/ui/issues/1750)) ([4299642](https://github.com/nuvla/ui/commit/4299642cbe4af1e74d0831238d17b53e39326ff9))


### Bug Fixes

* **Clouds page:** Fix icon for adding infra service to an infra service group ([2505e18](https://github.com/nuvla/ui/commit/2505e183f66eac43fcfca881fca84f6e50396105))
* **Credential page:** Pagination and table and bugfixes ([#1747](https://github.com/nuvla/ui/issues/1747)) ([7109d50](https://github.com/nuvla/ui/commit/7109d50511694a720f6d1e57231da6d2f385c39f))

## [2.47.1](https://github.com/nuvla/ui/compare/2.47.0...2.47.1) (2025-06-13)


### Bug Fixes

* **Edge details page:** COE resource action pull image private registries NE version &gt;=2.19.2 ([#1742](https://github.com/nuvla/ui/issues/1742)) ([339b7d4](https://github.com/nuvla/ui/commit/339b7d468a97123445c658484bd548f5c72df6fb))
* **Edge utils:** consider ne-dev as always a new version refactor utils ([339b7d4](https://github.com/nuvla/ui/commit/339b7d468a97123445c658484bd548f5c72df6fb))

## [2.47.0](https://github.com/nuvla/ui/compare/2.46.0...2.47.0) (2025-06-12)


### Features

* **NuvlaEdge detail page:** Allow selection of registry credential for pull coe resource action ([#1737](https://github.com/nuvla/ui/issues/1737)) ([5e53c06](https://github.com/nuvla/ui/commit/5e53c062b4c866b45e30101d750d18478ca1fcb9))


### Bug Fixes

* **cimi page:** Icon light theme for menubar ([fe9ffac](https://github.com/nuvla/ui/commit/fe9ffacae3798cfd9c338f6f9e9ab1c03f76dac3))
* **cimi page:** On collection change, remove existing entries ([fe9ffac](https://github.com/nuvla/ui/commit/fe9ffacae3798cfd9c338f6f9e9ab1c03f76dac3))
* **cimi page:** Search icon is loading when reaching the page bugfix ([fe9ffac](https://github.com/nuvla/ui/commit/fe9ffacae3798cfd9c338f6f9e9ab1c03f76dac3))
* **deployment-urls:** Make urls clickable only if the deployment is in STARTED state. ([#1740](https://github.com/nuvla/ui/issues/1740)) ([34f8114](https://github.com/nuvla/ui/commit/34f811404bcbaccbe89685bff6141f1082e88097))
* **general-utils:** Fix json parsing ([#1738](https://github.com/nuvla/ui/issues/1738)) ([fe9ffac](https://github.com/nuvla/ui/commit/fe9ffacae3798cfd9c338f6f9e9ab1c03f76dac3))
* **uix:** Reusable ModalActionButton component ([fe9ffac](https://github.com/nuvla/ui/commit/fe9ffacae3798cfd9c338f6f9e9ab1c03f76dac3))

## [2.46.0](https://github.com/nuvla/ui/compare/2.45.0...2.46.0) (2025-02-19)


### Features

* **Deployment group details page:** Make dynamic edge filter selectable from create page ([#1733](https://github.com/nuvla/ui/issues/1733)) ([a869dc0](https://github.com/nuvla/ui/commit/a869dc0e9de76d34516370430fe01eeeb83e78a7))
* **Edges:** Remove Bulk Deploy buttons from Edges page ([#1734](https://github.com/nuvla/ui/issues/1734)) ([d5eb484](https://github.com/nuvla/ui/commit/d5eb484339e0366f17f1abfec79f31d2fe9bc6c4))

## [2.45.0](https://github.com/nuvla/ui/compare/2.44.0...2.45.0) (2025-02-07)


### Features

* **Deployment group page:** In the DG details page the apps selection must be consistent with the DG subtype ([#1730](https://github.com/nuvla/ui/issues/1730)) ([a61ebb7](https://github.com/nuvla/ui/commit/a61ebb73546d119e6458a9351c67cb06dd063e97))


### Bug Fixes

* **CI:** Build dependencies upgrade ([#1728](https://github.com/nuvla/ui/issues/1728)) ([b59cbbd](https://github.com/nuvla/ui/commit/b59cbbd6e415074acff8e33f2c90f24ab049676e))
* **Deployment group details:** Summary stats ([a61ebb7](https://github.com/nuvla/ui/commit/a61ebb73546d119e6458a9351c67cb06dd063e97))

## [2.44.0](https://github.com/nuvla/ui/compare/2.43.0...2.44.0) (2025-01-31)


### Features

* **Deployment set:** Make edge selection consistent with DG subtype ([#1725](https://github.com/nuvla/ui/issues/1725)) ([639dc76](https://github.com/nuvla/ui/commit/639dc761b5006fb9f88fa94adbe0f1d50c6b795f))


### Bug Fixes

* **App bouquets:** fix validation error on creation ([#1727](https://github.com/nuvla/ui/issues/1727)) ([e167401](https://github.com/nuvla/ui/commit/e167401407a637c8ed36a8869e54a5a0a12bb90d))

## [2.43.0](https://github.com/nuvla/ui/compare/2.42.1...2.43.0) (2025-01-27)


### Features

* **Deployment details page:** Support remove-images, remove-volumes for docker compose on stop action ([#1717](https://github.com/nuvla/ui/issues/1717)) ([8d3dc00](https://github.com/nuvla/ui/commit/8d3dc008ba6ff878f2fa218e84222dd0672aa86c))
* **dg-sub-type-selector:** new component for the selection of DG sub type ([#1715](https://github.com/nuvla/ui/issues/1715)) ([6263e49](https://github.com/nuvla/ui/commit/6263e4901512b6c583864a8affdc367e63f1d88b))
* **resource-filter-comp:** Add new resource filter component ([#1718](https://github.com/nuvla/ui/issues/1718)) ([30ccc89](https://github.com/nuvla/ui/commit/30ccc8922e1f9d11f5b237f58ee718af8264a447))


### Bug Fixes

* **Apps details page:** Ask user if he wants to ignore ongoing changes when switching to different app version ([7d270c2](https://github.com/nuvla/ui/commit/7d270c26af8a8b214cec4e80b97a1034becd4fdc))
* **Apps details page:** Switching between app version is not refreshing helm fields ([#1719](https://github.com/nuvla/ui/issues/1719)) ([7d270c2](https://github.com/nuvla/ui/commit/7d270c26af8a8b214cec4e80b97a1034becd4fdc))
* **Apps details page:** Switching between generalize key to reload on module content change ([7d270c2](https://github.com/nuvla/ui/commit/7d270c26af8a8b214cec4e80b97a1034becd4fdc))
* **Apps details page:** Warning delete button disabled prop should be a boolean fix ([7d270c2](https://github.com/nuvla/ui/commit/7d270c26af8a8b214cec4e80b97a1034becd4fdc))
* **CI:** Deprecated github upload-artifact@v3 upgraded to upload-artifact@v4 ([92f4df6](https://github.com/nuvla/ui/commit/92f4df68de29209cbf29117151975ec5d6d281cf))
* **Cloudflare:** fix location for enable-2fa and disable-2fa ([c604209](https://github.com/nuvla/ui/commit/c604209bfa212d6183022fce36990c3bf934a861))
* **Cloudflare:** Two factor authentication stopped working bugfix ([#1710](https://github.com/nuvla/ui/issues/1710)) ([d07d892](https://github.com/nuvla/ui/commit/d07d89205e3b553c48e2e9ed4f1592441db9c53e))
* **Component tests:** Wait reset button to be visible before taking screenshots ([92f4df6](https://github.com/nuvla/ui/commit/92f4df68de29209cbf29117151975ec5d6d281cf))
* **Components test:** Reduce workers to 2 to avoid flakyness ([#1722](https://github.com/nuvla/ui/issues/1722)) ([92f4df6](https://github.com/nuvla/ui/commit/92f4df68de29209cbf29117151975ec5d6d281cf))
* **Components test:** Screenshot only sceneroot to avoid breaking when adding new scenes ([#1713](https://github.com/nuvla/ui/issues/1713)) ([da14712](https://github.com/nuvla/ui/commit/da147125488df9db5c4ad6476beecca3a4a20043))
* **Deployment dialog:** Reuse fetch job mechanism ([#1721](https://github.com/nuvla/ui/issues/1721)) ([12c9463](https://github.com/nuvla/ui/commit/12c94634135fbf3f2f596395049a96266c2ec569))
* **Deployment group details page:** Show save error message during creation phase ([#1720](https://github.com/nuvla/ui/issues/1720)) ([08829ab](https://github.com/nuvla/ui/commit/08829abfe714df001bd718e88e6aaae243da5ef8))
* **e2e:** Teardown redirection flaky test bugfix ([#1714](https://github.com/nuvla/ui/issues/1714)) ([d4f5530](https://github.com/nuvla/ui/commit/d4f55301350ed1af5489072d204dcd7252638488))
* **NuvlaEdge update modal:** When selecting a version with no/less optional modules, current ones will try to be installed bugfix ([#1723](https://github.com/nuvla/ui/issues/1723)) ([39d5ea0](https://github.com/nuvla/ui/commit/39d5ea04fac6df329829a075d07a488d532b4a68))
* **select-fields-icon-render-bug:** removing z-index from table select fields icon ([#1712](https://github.com/nuvla/ui/issues/1712)) ([0708550](https://github.com/nuvla/ui/commit/0708550a09cc8852065539de71a020725ef54d4d))
* **UIX:** Autocomplete one time code ([c604209](https://github.com/nuvla/ui/commit/c604209bfa212d6183022fce36990c3bf934a861))

## [2.42.1](https://github.com/nuvla/ui/compare/2.42.0...2.42.1) (2024-12-23)


### Bug Fixes

* **Bulk progress component:** job completed should be displayed as so in the progress bar header ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Bulk progress:** Bulk progress not visible when session is empty bugfix ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Bulk progress:** Display frequencies bootstrap errors and action c… ([#1700](https://github.com/nuvla/ui/issues/1700)) ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Bulk progress:** Show bulk progress for all users without filtering other users actions ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Bulk progress:** Support new format of status message with error_reasons ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **CI Components test:** cache key bugfix ([a93cb40](https://github.com/nuvla/ui/commit/a93cb40b2d71a1fb9ebbd34728fca4cc0e672de9))
* **Deployment group details page:** Start modal, update modal reset on confirm ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Deployment group details:** Render job cell related to bulk actions ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Edge detail page:** Improved "Resource Consumption" display ([#1704](https://github.com/nuvla/ui/issues/1704)) ([ade725b](https://github.com/nuvla/ui/commit/ade725bf73cdba24d95b39329d08fc2d93c18d69))
* **Edge detail page:** Show the new container stats table by default ([ade725b](https://github.com/nuvla/ui/commit/ade725bf73cdba24d95b39329d08fc2d93c18d69))
* **Edge detail page:** Use format-bytes to display disk capacity ([ade725b](https://github.com/nuvla/ui/commit/ade725bf73cdba24d95b39329d08fc2d93c18d69))
* **Edge details page:** Container stats table not update received values [#3443](https://github.com/nuvla/ui/issues/3443) ([#1707](https://github.com/nuvla/ui/issues/1707)) ([f67cd54](https://github.com/nuvla/ui/commit/f67cd54fb545b06c8f483044fd2d543bfe649626))
* **Github component test:** Not taking into account update-snapshots input bugfix ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Job tab component:** Hide scroll bar and break word instead ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **Table component:** Pagination style fix dropdown too much on the left in job tab table ([1af2f6d](https://github.com/nuvla/ui/commit/1af2f6dc6d49a6148ab11e689953dda501596b8c))
* **wrong_infra_selected:** Add type selector to DG creation page ([#1703](https://github.com/nuvla/ui/issues/1703)) ([1099dd0](https://github.com/nuvla/ui/commit/1099dd0490374a7e4d232a7bdc0d3fb4cf2dd2df))

## [2.42.0](https://github.com/nuvla/ui/compare/2.41.2...2.42.0) (2024-12-04)


### Features

* **Deployment group details page:** Make name&desc fields directly editable ([#1699](https://github.com/nuvla/ui/issues/1699)) ([e5b8865](https://github.com/nuvla/ui/commit/e5b88659eb9d5fcd430560312363cf179ee657fd))
* **Deployment Group page:** Auto-refresh checkbox should be there at creation time before save ([c76f734](https://github.com/nuvla/ui/commit/c76f734b1f82fb15734d3bff6bfe60914c13fdbf))
* **dg-jobs-tab:** Add a jobs tab related to DG ([#1696](https://github.com/nuvla/ui/issues/1696)) ([1661638](https://github.com/nuvla/ui/commit/1661638290f1e25cab3db054b322b228b5bf219f))


### Bug Fixes

* **Deployment details page:** Detach action do no refresh page bugfix ([#1693](https://github.com/nuvla/ui/issues/1693)) ([da5357e](https://github.com/nuvla/ui/commit/da5357ea99fa20bfec2b8381bd7355e7ccd93efc))
* **Deployment group details page:** Set dynamic filter button remove edges stats in overview ([#1694](https://github.com/nuvla/ui/issues/1694)) ([d76414a](https://github.com/nuvla/ui/commit/d76414a44458112f22a74e1f15d9c6e871559e0f))
* **Deployment group details:** Always display auto-update fields ([#1701](https://github.com/nuvla/ui/issues/1701)) ([66af2b6](https://github.com/nuvla/ui/commit/66af2b6c4fa4410b2924e3dee74e1b0eb362b656))
* **inconsistent-msg-starting-dg:** also consider deployments-to-update ([#1698](https://github.com/nuvla/ui/issues/1698)) ([2a539c6](https://github.com/nuvla/ui/commit/2a539c6e5d7487b20302bbb44425cab136c7cf05))

## [2.41.2](https://github.com/nuvla/ui/compare/2.41.1...2.41.2) (2024-11-26)


### Bug Fixes

* **Deployment group detail page:** Make stats clickable only when items are available ([#1689](https://github.com/nuvla/ui/issues/1689)) ([25247d9](https://github.com/nuvla/ui/commit/25247d9ce37adaf533c29c23f0ea524db1304809))
* **dg-overview-layout:** use css grid for duration picker ([#1690](https://github.com/nuvla/ui/issues/1690)) ([4fc869c](https://github.com/nuvla/ui/commit/4fc869c0194dd14769968030190b7b4d2f5b5fe7))

## [2.41.1](https://github.com/nuvla/ui/compare/2.41.0...2.41.1) (2024-11-25)


### Bug Fixes

* **Deployment group pages:** Add experimental warning banner ([#1686](https://github.com/nuvla/ui/issues/1686)) ([c2fe2d5](https://github.com/nuvla/ui/commit/c2fe2d5f682835481c34318877decf6dac37c134))
* **Deployment group pages:** table view pagination bugfix ([01fa246](https://github.com/nuvla/ui/commit/01fa246de600acefa40a210d20e10f6c365a4223))
* **Deployment group page:** Use full width table ([01fa246](https://github.com/nuvla/ui/commit/01fa246de600acefa40a210d20e10f6c365a4223))

## [2.41.0](https://github.com/nuvla/ui/compare/2.40.1...2.41.0) (2024-11-20)


### Features

* **Deployment group detail page:** On refresh, auto-update ui content when DG edit page is not in dirty state ([#1680](https://github.com/nuvla/ui/issues/1680)) ([f173d1f](https://github.com/nuvla/ui/commit/f173d1f60bdb3fe022153ef821feb6ac69104fbe))
* **Deployment group detail page:** Support for auto update at custom interval ([4aff51f](https://github.com/nuvla/ui/commit/4aff51faec2ee54e33ed7878cc8051026a4e16e4))
* **Deployment group page:** Set default view to table view and use new table component ([4aff51f](https://github.com/nuvla/ui/commit/4aff51faec2ee54e33ed7878cc8051026a4e16e4))
* **dg-no-apps-no-edges:** Allow DGs to have no edges and / or no apps defined ([#1678](https://github.com/nuvla/ui/issues/1678)) ([524a329](https://github.com/nuvla/ui/commit/524a329c32104061b179b5266d1a11d8a912fcdb))


### Bug Fixes

* **Apps detail page:** Minimum requirements architectures bugfix ([#1675](https://github.com/nuvla/ui/issues/1675)) ([58acbb7](https://github.com/nuvla/ui/commit/58acbb74386e8defff9e161bf5b8c686f9479336))
* **Cimi api page:** Nuvlabox update modal releases not filled when reloading the page ([64af079](https://github.com/nuvla/ui/commit/64af07983799092bf8852b2d77ac1fee51a11e53))
* **Component tests:** Flaky test fix because of missing await ([e429a4e](https://github.com/nuvla/ui/commit/e429a4eef1c4b90d9ca31ec8ceb9b0cba58d9209))
* **e2e:** Deployment group use random id to avoid breaking when a precedent test run break ([544512e](https://github.com/nuvla/ui/commit/544512edb4a26296774c8afe56665b0b507ce083))
* **Edge details page:** Support for helm releases coe resources ([#1681](https://github.com/nuvla/ui/issues/1681)) ([b2216ec](https://github.com/nuvla/ui/commit/b2216ec8e2932e54bfff2072e55d630b7b672140))
* **Edge details page:** Support Kubernetes coe resources ([443fa4c](https://github.com/nuvla/ui/commit/443fa4c55a1db91f1c836a94b732853fb930c125))
* **Edge details page:** Support Kubernetes coe resources raw data view ([15c1b2c](https://github.com/nuvla/ui/commit/15c1b2c7c7b0267c92d88b0b874ca832dead8094))
* **Edge details:** When returning on Edge after some times, we should not show telemetry outdated message before that the status is retrieved ([#1670](https://github.com/nuvla/ui/issues/1670)) ([79e1cd8](https://github.com/nuvla/ui/commit/79e1cd8b600c6a9248fc6e7f2a7d841b2ea778bc))
* **Editor component:** Force parsing within 100ms to handle folding big documents ([#1677](https://github.com/nuvla/ui/issues/1677)) ([fe8dc18](https://github.com/nuvla/ui/commit/fe8dc18e22811310dc9ce3b9c979749a528d2ae3))
* **fontawesome:** Host fontawesome pro 6.6.0 ([6c4a7e1](https://github.com/nuvla/ui/commit/6c4a7e16e51733effe56dab4cd99fd2ae87d3c14))
* **Nuvlaedge details page:** Update modal data not refreshed ([#1671](https://github.com/nuvla/ui/issues/1671)) ([64af079](https://github.com/nuvla/ui/commit/64af07983799092bf8852b2d77ac1fee51a11e53))

## [2.40.1](https://github.com/nuvla/ui/compare/2.40.0...2.40.1) (2024-10-21)


### Bug Fixes

* **app-compatibility:** Set docker-compose as default compatibility on new apps ([#1665](https://github.com/nuvla/ui/issues/1665)) ([63730a8](https://github.com/nuvla/ui/commit/63730a87545000836dd746feee0346a398b00ca5))
* **Apps detail:** Minimum requirements make application page crash when in read-only mode ([#1667](https://github.com/nuvla/ui/issues/1667)) ([c5478a6](https://github.com/nuvla/ui/commit/c5478a6f7f95c779995d03a8d013c40650f92737))
* **UIX:** TableRowCell avoid crashing when value is a collection ([c5478a6](https://github.com/nuvla/ui/commit/c5478a6f7f95c779995d03a8d013c40650f92737))

## [2.40.0](https://github.com/nuvla/ui/compare/2.39.1...2.40.0) (2024-10-18)


### Features

* **audit-logs-new-table:** Replace audit logs table with refactored one ([#1662](https://github.com/nuvla/ui/issues/1662)) ([65a3c3c](https://github.com/nuvla/ui/commit/65a3c3c38e77d64083f41af57ebff45cbdef3567))
* **Edge detail page:** Support for actions on coe-resources ([2d7302d](https://github.com/nuvla/ui/commit/2d7302da41d28b380b17a68011d34bd84adf4a97))
* **Portfolio:** Integrate with the Portfolio library ([2d7302d](https://github.com/nuvla/ui/commit/2d7302da41d28b380b17a68011d34bd84adf4a97))
* **Table-refactor:** Rewrite of table with testability and usability in mind ([2d7302d](https://github.com/nuvla/ui/commit/2d7302da41d28b380b17a68011d34bd84adf4a97))


### Bug Fixes

* **Application:** Fix helm app check ([#1652](https://github.com/nuvla/ui/issues/1652)) ([6d4acd8](https://github.com/nuvla/ui/commit/6d4acd8b3dcf2fad77f312634738dbe80a98a99b))
* **Apps detail page:** add missing architectures ([21ce23f](https://github.com/nuvla/ui/commit/21ce23f3949ef025d51ef2cded57fa84b85e394a))
* **Edge detail page:** Load nuvlabox status partially depending on current tab ([50da983](https://github.com/nuvla/ui/commit/50da9837590a517c851675eea58957824a8a3ac4))
* **Edge detail page:** Remove popup from resource consumption tab selector ([50da983](https://github.com/nuvla/ui/commit/50da9837590a517c851675eea58957824a8a3ac4))
* **Edge detail page:** Use table refactor for resource consumption new stats table ([2d7302d](https://github.com/nuvla/ui/commit/2d7302da41d28b380b17a68011d34bd84adf4a97))
* **edge-page:** Job display enhancement for failed and in progress by reusing components ([331274d](https://github.com/nuvla/ui/commit/331274d8a5dee4a619dc3ab4074cef34a77486f0))
* **Github workflow:** Install lein missing in ubuntu 24 ([#1657](https://github.com/nuvla/ui/issues/1657)) ([cbaa17c](https://github.com/nuvla/ui/commit/cbaa17ca8f2014d3eca2bd5703341ba0371b7fe4))
* **min-req:** arch64 -&gt; aarch64 ([#1659](https://github.com/nuvla/ui/issues/1659)) ([a6791cd](https://github.com/nuvla/ui/commit/a6791cdbef7d52722a15460003245dcac5141f13))
* **min-req:** Only show check icon if min requirements are met or unmet requirements are accepted ([#1658](https://github.com/nuvla/ui/issues/1658)) ([3ba5ffc](https://github.com/nuvla/ui/commit/3ba5ffc62c3541ea3479ea31e48eaa857d8d9e16))
* **nuvlaedge-self-registration.py:** fix misusage of rstrip. ([0a0be1c](https://github.com/nuvla/ui/commit/0a0be1cd7d22b20160908a019594840f37536f7e))
* **Switch group:** Check for unsaved changes before switching group ([#1651](https://github.com/nuvla/ui/issues/1651)) ([d4a86a7](https://github.com/nuvla/ui/commit/d4a86a774707af1ec87438c08804efc860b3f75a))
* **table-refactor:** select all checkbox should only select the elements in the current page ([e7ad2a2](https://github.com/nuvla/ui/commit/e7ad2a27887efa69e154852517aaa11dc0d43366))
* **Ui-demo:** Retire internal ui demo page replaced by Portfolio ([e9e16dc](https://github.com/nuvla/ui/commit/e9e16dc367ee59435de4c4f94f8b7b447d295c76))


### Dependencies

* **Dnd-kit:** Add drag and drop utilities for react ([2d7302d](https://github.com/nuvla/ui/commit/2d7302da41d28b380b17a68011d34bd84adf4a97))


### Minor Changes

* **nuvlaedge-self-registration.py:** compatibility with python &lt;3.10 ([0a0be1c](https://github.com/nuvla/ui/commit/0a0be1cd7d22b20160908a019594840f37536f7e))
* **nuvlaedge-self-registration.py:** fallback to socket.gaierror if NameResolutionError is not available in urllib3 (&lt; v2.0.0) ([feb0bbf](https://github.com/nuvla/ui/commit/feb0bbf11b4118687790a46a3fa12f922c0fc872))
* **nuvlaedge-self-registration.py:** in case of error, return exit code 1 ([322eba4](https://github.com/nuvla/ui/commit/322eba488c39a68630856672a901418d1419b1a2))


### Continuous Integration

* **playwright-snapshot:** Allow testing by snapshots ([331274d](https://github.com/nuvla/ui/commit/331274d8a5dee4a619dc3ab4074cef34a77486f0))

## [2.39.1](https://github.com/nuvla/ui/compare/2.39.0...2.39.1) (2024-09-26)


### Bug Fixes

* **Changelog:** Remove unreleased section ([a01069e](https://github.com/nuvla/ui/commit/a01069e406a6e0c7da4ef4fd970ac42fcff82048))
* **Edge details page:** Do not show Docker tab when no data in nuvlabox status ([e3a5307](https://github.com/nuvla/ui/commit/e3a53079305e4297b425a96323bc0cb9a335286c))

## [2.39.0](https://github.com/nuvla/ui/compare/2.38.1...2.39.0) (2024-09-25)


### Features

* **App:** App minimum requirement + resource check at deployment time ([#1624](https://github.com/nuvla/ui/issues/1624)) ([886b8bb](https://github.com/nuvla/ui/commit/886b8bbfb7a292ebb0848893a742f034fe633825))
* **Deployment Group Detail:** Add minimum requirements check to deployment groups start&update modals ([886b8bb](https://github.com/nuvla/ui/commit/886b8bbfb7a292ebb0848893a742f034fe633825))
* **Deployment Modal:** Requirements warning ([886b8bb](https://github.com/nuvla/ui/commit/886b8bbfb7a292ebb0848893a742f034fe633825))
* **Edges details:** Support Docker resources  ([#1632](https://github.com/nuvla/ui/issues/1632)) ([932dd3a](https://github.com/nuvla/ui/commit/932dd3a2a2aa425e09761711fbf9fae278b4e675))


### Bug Fixes

* **Bulk progress plugin:** Change cancel icon ([f6f005a](https://github.com/nuvla/ui/commit/f6f005a5682517429d2cd24122e68673f5ea10ce))
* **Deployment Group Detail:** fix e2e tests ([886b8bb](https://github.com/nuvla/ui/commit/886b8bbfb7a292ebb0848893a742f034fe633825))
* **Deployments page:** Deployment bulk delete remove without forcing deletion ([#1630](https://github.com/nuvla/ui/issues/1630)) ([44b028a](https://github.com/nuvla/ui/commit/44b028ae1730e9ad2c4c8eb3db8b284a2d6d1589))
* **dg-min-req:** check disk with maximum available space ([#1631](https://github.com/nuvla/ui/issues/1631)) ([4e1aac4](https://github.com/nuvla/ui/commit/4e1aac443ae7322af2500e7375b0f3141444b809))
* **Edge page:** Remove double confirmation for bulk update action ([f6f005a](https://github.com/nuvla/ui/commit/f6f005a5682517429d2cd24122e68673f5ea10ce))
* **Global:** Visual change flat without shadow inconsistency ([#1629](https://github.com/nuvla/ui/issues/1629)) ([ad0aec9](https://github.com/nuvla/ui/commit/ad0aec9933681e729cf468c4e85775d012602dd9))
* **kvlt:** Monkey patch kvlt.platform.xhr/req-&gt;url to avoid IPv6 address in server name ([#1634](https://github.com/nuvla/ui/issues/1634)) ([6925c6c](https://github.com/nuvla/ui/commit/6925c6c9c64fb17b06fa8474a873f0c7472c6f3a))
* **UIX:** ButtonAskingForConfirmation support icon and move action text to button ([f6f005a](https://github.com/nuvla/ui/commit/f6f005a5682517429d2cd24122e68673f5ea10ce))
* **uix:** New option show-pencil? for TableRowCell ([886b8bb](https://github.com/nuvla/ui/commit/886b8bbfb7a292ebb0848893a742f034fe633825))


### Minor Changes

* **nuvlaedge-self-registration.py:** allow to manually define mac address and improved output (steps go to stdout, details and debugs go to stderr) ([#1626](https://github.com/nuvla/ui/issues/1626)) ([063f6b3](https://github.com/nuvla/ui/commit/063f6b395fbed85631ba2fc5f28cd7e4c0f59208))

## [2.38.1](https://github.com/nuvla/ui/compare/2.38.0...2.38.1) (2024-08-09)


### Bug Fixes

* add missing import of time from sixsq tools ([09a9785](https://github.com/nuvla/ui/commit/09a978500590abe53d16f4b3d26ae54a049a379b))
* **cloudflare:** NE not accessible when location is in the body ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **Edge detail:** Do not show error message for NE go version or for NE dev versions ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **Edge details:** Show error message when user is trying to upgrade between major versions ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **edge details:** Show message informing users coming from 2.14.3 or earlier to upgrade to v2.14.4 first ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **Edge detail:** Target version is not set bugfix ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **Edge page:** Do not show peripheral section when empty list ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **Edge page:** Make target release dropdown bigger ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **Edge page:** NE release ordered by release-date attribute ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **Edges utils:** Remove unused function ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))
* **readme.md:** Add docker image size badge ([88b03e5](https://github.com/nuvla/ui/commit/88b03e57ed9e322c12f4e8b47f01af99ad068b01))
* **semver:** Extract and refactor semver parsing into an utilities namespace ([3153bbf](https://github.com/nuvla/ui/commit/3153bbfd1a451685f776d626253ebf1e28f91124))


### Continuous Integration

* **ci:** Fix docker image build time for release workflow ([4e015cc](https://github.com/nuvla/ui/commit/4e015ccc0dc1b409c4b12016f95a8324cf4b9fa0))
* **ci:** Rename NuvlaBox to NuvlaEdge ([68607a4](https://github.com/nuvla/ui/commit/68607a49539917e786a8c0ca2108b9d80b7de4be))
* **ci:** Reuse GitHub workflow code between dev and release ([#1610](https://github.com/nuvla/ui/issues/1610)) ([1ccdf75](https://github.com/nuvla/ui/commit/1ccdf7527153aaa66dadd32899cc4fcce9e82003))
* **ci:** Run unit tests, bundle size check, and e2e tests also in release PRs ([#1612](https://github.com/nuvla/ui/issues/1612)) ([fde04d6](https://github.com/nuvla/ui/commit/fde04d60f414ed8d03b0303863cde93468a98738))
* **github:** rename Release Please private key secret to RELEASE_PLEASE_PRIVATE_KEY ([#1618](https://github.com/nuvla/ui/issues/1618)) ([62218f1](https://github.com/nuvla/ui/commit/62218f1095ec324829cb0fa933f1a28497256ec3))
* **github:** use GitHub App token to run release-please ([#1615](https://github.com/nuvla/ui/issues/1615)) ([68cf15f](https://github.com/nuvla/ui/commit/68cf15ffc8cd94e110e05978136a97c601c80259))
* **github:** use GitHub App token to run release-please ([#1616](https://github.com/nuvla/ui/issues/1616)) ([3897354](https://github.com/nuvla/ui/commit/3897354a1ff50054650de16836f77008d108e15d))
* **github:** use GitHub App token to run release-please ([#1617](https://github.com/nuvla/ui/issues/1617)) ([575dc2b](https://github.com/nuvla/ui/commit/575dc2b284579220d6dbe08bcfb4c1e785b9ca3f))

## [2.38.0](https://github.com/nuvla/ui/compare/2.37.3...2.38.0) (2024-07-25)


### Features

* **Helm Apps:** Support creation and configuration of Helm Apps ([#1572](https://github.com/nuvla/ui/issues/1572)) ([a525241](https://github.com/nuvla/ui/commit/a525241cc9c2653f765142480fe88bb9db6a6c3a))

## [2.37.3](https://github.com/nuvla/ui/compare/v2.37.2...2.37.3) (2024-07-25)


### Continuous Integration

* **ci:** e2e test cloudflare fix ([5aaf97f](https://github.com/nuvla/ui/commit/5aaf97f0663db60084fd8757ae1893fc603c8753))
* **ci:** update bootstrap-sha ([c551707](https://github.com/nuvla/ui/commit/c55170715aef6d3c1403326d76c4888cc41c14a0))

## [2.37.2](https://github.com/nuvla/ui/compare/v2.37.2-SNAPSHOT...2.37.2) (2024-07-25)


### Continuous Integration

* **github:** Github orchestrate build of ui docker image ([19350fb](https://github.com/nuvla/ui/commit/19350fb600b6cf2c48ef36c4752f464acb3598b7))
* **github:** Upgrade plugins ([19350fb](https://github.com/nuvla/ui/commit/19350fb600b6cf2c48ef36c4752f464acb3598b7))
* **maven:** Retire maven ([#1596](https://github.com/nuvla/ui/issues/1596)) ([19350fb](https://github.com/nuvla/ui/commit/19350fb600b6cf2c48ef36c4752f464acb3598b7))
* **release-please:** Release please control version based on commit messages ([19350fb](https://github.com/nuvla/ui/commit/19350fb600b6cf2c48ef36c4752f464acb3598b7))
* **travis:** Remove reference to Travis ([19350fb](https://github.com/nuvla/ui/commit/19350fb600b6cf2c48ef36c4752f464acb3598b7))

## [2.37.1] - 2024-06-18

- Notifications - Use content-type for data-records instead of tag

## [2.37.0] - 2024-06-18

- Notification page - Add mqtt notif method
- Remove the feature of the creation of clusters with COE
  - Clouds page
    - Remove coe provioning section
    - Add link to credentials page in add modal
  - Credentials page - Remove cloud service credentials support
  - Clouds details page - Remove terminate button and logic around editable endpoint
- Credential page - Display credential ID and provide possibility to copy it
- Clouds page - Show stop action only when available through operations attribute
- Job component
  - Message not displayed to the user when user is already on job tab while job display message is being
  set fix
  - Change page to 1 on mount navigation to avoid sideffect on remount for another deployment or edge bugfix

## [2.36.9] - 2024-05-24

- Edges detail page - NE version regression bugfix

## [2.36.8] - 2024-05-23

- Resource log component - List of components show only workload object kinds
- Edges detail page
  - Form fields are not prefilled with install params bugfix
  - NE version not refreshing when changed in resource bugfix
- Job component - Improve the job message display available in the table

## [2.36.7] - 2024-05-16

- Welcome page - Update welcome video and allow full screen
- Application page - Make App list and button wrap inside container so that it doesn't fall off the screen
- Deployment group page - Set default name and description when creating
- Deployments page
  - Add button to create deployment group
  - Shutdown->stop
- Deployments detail page - Shutdown->stop
- Deployment modal - Encourage users to check out deployment group feature
- Edges page
  - Release warning sign bugfix. Latest version should exclude pre-release
  - Always show bulk actions in NE table view, enable/disable buttons when applicable
  - Make bulk actions bar always visible
    - Fix deploying with dynamic edges
    - Only enable bulk deploy when there are selected edges
    - Minor stylistic improvements for filter display component
- Edges detail page - Always display 'Update' button
  - Extract version warning display to its own component, move it to utils
  - Enable update of a NE without refreshing/switching views
  - Improve styling of edge availability pop-up
    - Fix minimum width for NE availability card, make table not overflow parent div
    - Make chart and NE-card stack on small screens

## [2.36.6] - 2024-03-28

- Edges page - Add indicator next to version number in NuvlaEdges view to indicate whether a version is outdated
- Edges page - Availability history generate x-axis ticks based on data, for NE availability graph
- UI Refactor
  - Group pages in a namespace
  - Group common components into namespace
  - Add layout notion
  - protected? page logic moved to the router and layout concept
  - Fix protected pages redirections and authentication page redirect
  - Use authentication (and logout) strategy in end-to-end tests

## [2.36.5] - 2024-03-21

- Deps - Added chartjs-adapter-date-fns to format dates in charts
- Edges detail page - NuvlaEdge historical view
- Edges page - NuvlaEdge fleet historical availability view
- Edges detail page - update NE modal align with add modal
- Edges page - add NE modal - Do not select additional modules by default at NE creation, and update display
- Apps page - Add Files section to DGs App configuration page
- Deployment group page - Add Files section to DGs App configuration page
- Edges page - Fix race condition in fetching edges when creating DG with dynamic filter
- Notifications page - Query params based browsing for tabs

## [2.36.4] - 2024-02-21

- Notifications page
  - Tabs browsing via query params
  - Allow user to trigger notification test
- Deployment detail page - Version update of an existing deployment bugfix
- Deployment group
  - Display last updated and operational status on card
  - On automatic page refresh we loose unsaved changes
  - Reload DG apps on save
  - Fix inconsistency of dynamic filter definition and number of edges
- Profile page - Add coupon modal hang on error fix
- Acl - Principal show error message when not entering a group or a user id
- UIX - CopyToClipboardDownload click to copy not working bugfix
- Make statistics component accurately represent all states
- Review e2e tests and attempt to fix flaky tests
- Build version - support Github and Cloudflare commit short id extraction

## [2.36.3] - 2024-01-29

- Enable host-level management for Kubernetes-deployed NuvlaEdges

## [2.36.2] - 2024-01-18

- Fix for missing registries credentials on deployments linked to DGs
- Make action check not deployment-specific

## [2.36.1] - 2023-12-21

- Tags component - Fix alignment of check button
- Deployment table
  - Show URL in deployments table only when deployment is in STARTED state
  - Application link move to deployment details instead of application details
- App detail
  - Do not allow publish or unpublish an app version with unsaved changes
  - Do not show warning about unsaved changes while editing a new app
- Deployment groups
  - Remove deployment-set* routes
  - Get nuvlaedge engine version from resources, if present
  - Enrich apps with version prop on init
  - Forcing modules reload on refresh
  - Clear the overrides when changing the version of the app bouquet
  - Fix start/stop/update DG job descriptions
  - Reset Deployment Group start/update modals
  - Hide version warning and remove icon from apps overview pane in App Bouquet based DGs
  - Add e2e test

## [2.36.0] - 2023-12-14

- Job - Do not show job error message after subsequent successful deployments
- Nuvlaedge detail - Fix display of 'Last boot' time on NuvlaEdge page
- Notification configurations for the case of publication of applications
  - Add definition of application publication notifications
  - Module publish event criteria docu string
- Module - Make the user that copies an app
- Module app-set
  - Overwrites not taken into account when updating from a specific module version
- Deployment group
  - Handle empty env var and reset to parent value separately
  - Handle back button in DG details page
  - Remove experimental flag for deployment group
  - Stop message not counting correctly all deployments to be stopped
  - Fix for: emptying a non-mandatory variable returns 400 error
  - Highlight mandatory empty dropdown fields
  - Show the same action buttons in Deployment Group creation and edition pages
  - Fix for: When adding/removing apps in depl group links to marketplace are broken
  - Adds apps link to app name in deployments table and make deployments table behave like other tables
  - Deployment group creation/edition supports application sets correctly
  - EULA and pricing info added to Start and Update dialogs
  - Enabled edge picker when creating deployment group
  - Fix issue with quantity
  - Enables select all bulk deployment from edges page
  - Review Apps Config tab layout
  - Rename Deployment Sets to Deployment Groups
  - Rename Application Sets to Application Bouquets
  - Warning added when app version is behind latest published version
  - Warning added to app set behind latest version
  - Add version warning to application bouquet details page
  - Allow selection of the elements on the deployments page via query parameters
  - Enhance message when deleting or updating/starting/stopping a deployment group
  - and many more check commit logs ...
- Edges add modal - switch to "docker compose"

## [2.35.3] - 2023-11-10

Deployment group detail - Fix permissions check on dep group creation (#1446)

## [2.35.2] - 2023-11-10

- Profile - Groupe remove member bugfix

## [2.35.1] - 2023-11-10

- Profile - Invitation to group not working regression (#1445)
- All edit actions should be disabled when edit operation is not allowed on
  deployment group (#1442)

## [2.35.0] - 2023-11-09

- Global - Use values new type of query in UI
- Edges
  - Add button Deploy App with Dynamic Edges
  - Fix console errors and warnings
- Deployment group detail
  - Adapt edge picker functionality to support dynamic edge filters
  - ModalDanger for start stop cancel should clause when
    action call is successful
  - When there are no deployment groups yet, we show a plus button in the middle
    of the page
  - Remove events plugin not used and making queries
  - Fix a bunch of console errors and warnings

## [2.34.2] - 2023-11-01

- Hotfix/job table messages (#1434)

## [2.34.1] - 2023-11-01

- Table component
  - Fixes ordering on restoring defaults (#1432)
  - Removes empty column header (#1430)
- API page - row don't show api link when for nil value
- Edge page - Optimize the query done to retrieve cluster nodes (#1431)
- Edge picker and minimal changes indicator (#1423)
- Deployment group detail page
  - Cancel operation button (#1429)
  - Lighter warn message on start/stop/update/cancel actions

## [2.34.0] - 2023-10-27

- Table plugin - Issue 1014 configurable tables columns (#1410)
- Demo page - Adds hidden internal UI demo page
- Removes rights-needed parameter from subscribe page-selected call (#1424)
- Cloud detail - Hide swarm disabled popup from deploy modal
- Filter parser - Align grammar with api-server and optimization use defparser (#1425)
- Filter comp - Impossible to set attribute value to false bugfix
- Filter comp - Removes acl attributes from filter options
- Deployment - Allow to sort by Edge device (#1408)
- Deployment detail - Shutdown modal do not show schedule word anymore on primary action button
- Deployment detail - Show NuvlaEdge status color (#1418)
- Deployment detail - Remove not useful deployment state header (#1419)
- Edges - Bulk Deploy App should create deployment group based on edge filter when edge selection is filter based
- Edge - Use next-heartbeat instead of computing it from refresh-interval bugfix (#1413)
- Edge - Better retro compatible display of last-online
- Edge detail - Do not show Heartbeat edition when Nuvlaedge hasn't heartbeat capability
- Edge detail - Better retro compatible display of last and next telemetry
- Deployment group - Add or remove applications from deployment group
- Deployment group detail - Show action Recompute fleet when the server makes it available
- Add additional filters to edges summary state
- Fix url to vpn documentation
- Saving after changing protection was enabled can lead to unexpected navigation to previous page (#1411)
- Deployment modal - Do not show schedule word anymore on primary action button (#1416)
- Deployment modal - Enhance visibility of selection on selected crede… (#1417)
- Deployment group detail - Delete modal wait job to finish (#1390)
- Deployment group detail - Delete modal group delete and force delete actions
- Unify state filter icons design and behavior (#1399)
  - Shows 0 in statistic value instead of "-"
  - Grey color if 0
  - Highlight "Total" in edge card depl group details
- Deployment modal stop checking credentials (#1404)
  - Credentials components - removed
  - Deployment modal - Do not check credentials
- Deployment set detail page - Select version not working in apps tab (#1384)
- Pull only - use ne status new fields and do not set execution-mode from ui (#1382)
- Deployment modal - do not set execution-mode of deployment (wip)
- Deployment modal - directly assume pull mode if it is supported HelpPopup, FieldLabel, NE telemetry heartbeat wip
- Edge detail - status telemetry use new fields
- Components - EditableInput support type and label
- Edge detail - On edit error reload old data from db
- Edge detail - refresh-interval and telemetry-interval label and numeric type
- Components - Fix EditableInput to allow value to go back to the document value on error and simplify
- Deployment group detail page - Highlight ui regions with validation errors
- Deployment group detail page
  - Better detection of editions
  - Save button enable logic improved
  - changed-env-vars now keeps all changed values (even empty strings)
  - Report validation errors in subscription. Extract menu item component capable of showing validation errors in popover.
  - Add mandatory parameter check on env vars
- Deployment group - Remove duplicated icons in depl group create page
- Renames "Navigate Apps" "Navigate Projects" (#1396)
- Adds action specific warning messages (#1394)
- Deployment group detail page - Save and other actions enabled at the same time
- Deployment group (#1397)
  - Sort on deployment set name in deployments table
  - Do not show "App" in apps table
  - Shorten popup message to "Open app details"
  - Adds icons to tabs
- Deployment group detail - Removes private container registries from apps config (#1388)
- Apps link in app card navigates to app config (#1386)
  - Moves app marketplace link to new column
  - Arrow icon for marketplace link
- Fixing occasional crash on deployments page (#1385)
- Depl groups misc (#1381)
  - Translates app-name to Name
  - Adds ops status color to page header icon
  - Adds deployment-set column to deployment table
  - Hides depl group link column on depl details page
  - Fetches deloyment set names for deployments table
- Deployment set detail page - Display version date in app version tooltip
- Display elapsed time from creation in tooltip
- Info icon shown next to version in apps table to indicate the presence of extra info
- Fix depl state filter (#1379)
- Deployment set detail page - Display version date in app version tooltip
- Deployment set detail page - app-name should be name in apps list
- Deployment set detail page - Deployment stats buggy, showing deployments while creating

## [2.33.14] - 2023-09-25

- Sticky menu bar - Adds max-items-to-show to RefreshMenuBar
- Apps - Hide automatically created apps-sets from my apps (#1361)
- Deployment group - Update button
- Deployment group - Changes protection
- Deployment group - Save button enabled when changes applied
- Deployment group - Removes automatic saving when editing details
- Deployment group - Action button highlighted when callable
- Deployment group - Disable actions based on available operations on resource
- Deployment group - App picker cards
- Deployment group - Configure apps and detection
- Deployment group - Reset filter when clicking "Show me"
- Deployment group - Adds operational status summary to depl state overview
- Deployment group - Removes ProgressJobAction, only keeping MonitoredJobs
- Deployment group - Statistics and cards adapted to new state machine
- Deployment - Shows updated column in deployments table
- Session - Display current group name instead of id on the top bar

## [2.33.13] - 2023-09-05

- Deployment groups page
- NuvlaEdge page - Sorts nuvlabox releases in add modal

## [2.33.12] - 2023-07-28

- Copy to clipboard buttons doesn't work in all places bugfix tasklist#2491

## [2.33.11] - 2023-07-21

- Show apps subscriptions invoices current usage #1331
- Api input field bug fix: Encode URI component #1341
- Do not display error message when job is requeued because of mixed mode #1343
- Job component - Progress bar for running job is red when re-queued fix.
- Job view - Hide job status-message from jobs table when job is queued
- Api page - refresh the content after switching to a usergroup #1333
- Deployment details - Remove billing tab #1334
- Deployment details - Remove billing tab
- Deployment modal - Remove coupon support
- Adds remove button to filter indicator #1339
- Change order of info popup and filter string
- Shows X-mark button to remove filter
- Container - configure cache in nginx for css js images and html
- version css replaced by version file
- SpanVersion component
- Hash main assets to force browser reload on file change (cache busting) #1336
- New UI release indicator is not displayed in all cases #1330
- Build - Append version number and hash to nuvla-ui.js
- Build - Regenerate index.html with versionned js name
- Build - Vesion.css generation move to shadow build hooks
- Build - Assets moved to shadow build hooks to avoid duplication
- Deps - Remove nbb (node babashka) dependency
- Deps - Upgrade highlight.js to avoid warning in console
- project.clj - Simplify
- Disables delete button for modules with children #1328
- Fix clipboard icon #1329
- Fixes add button icon in playbooks tab
- Makes projects_create test more stable

## [2.33.10] - 2023-06-28

- Filter comp - Better indication that a filter is active #1308
- Apps details - Always validate form before opening save modal
- Apps details - Do not load deployments for project
- Apps details - Remove refresh button
- Apps details - Refresh app resets edited fields during edition
- Edges details - Navigation to NE without view right or to non-existing one make
  UI crash in dev mode fix

## [2.33.9] - 2023-06-28

- Apps - Creation of project bugfix, content not allowed
- Edges - Sort by version number descending
- Router - Reload router components not working fix
- Deps - Upgrade clojure deps
- Readme - Document how to run unit tests in repl

## [2.33.8] - 2023-06-26

### Changed

- Apps - Clearer distinction of deletion modals between deployments and apps #1283
- Deployments - Clearer distinction of deletion modals between deployments and apps #1283
- Edge - Adapt UI for adding new k8s nuvlaedge #1243
- Map - Fixes map on cf preview deployments
- Deployment - Show nuvlaedge status the deployment is running on #984
- Deployment set detail - Save replace create action
- Deployment set detail - Remove deployment section and replace it by a redirect
  to deployments page with a filter
- Deployment set detail - Remove configuration empty section
- Deployment modal - Use module plugin for env cred dropdown
- Applications set - Support for env var cred selection
- Deployment set - Support for env var cred selection
- Module plugin - Bug fix regarding changed env values when empty string
- Applications sets - Use Tab instead of accordion for sets and applications
- Applications sets - Make new applications sets button less visible
- Applications sets - By default name the first set Main
- Applications sets - Select applications modal title changed
- Applications sets - Applications title aligned with the rest of application
  tabs
- Deployment set new - Create start button moved to a menu bar
- Deployment set new - Step completed checkmark
- Deployment set new - Step Eula/Price merged
- Deployment set new - Step Apps/Targets merged with configration
- Deployment set new - Require at least one target to be selected
- Deployment set new - Step icons changed
- Edge - Filter wizard works on edges map view
- Edge - Makes map view in edges page higher
- Edge details - Fixes font in edge host info ssh key dropdown
- Apps sets creation - Helper text that explains user what he need to do to
  create an apps sets
- Step group plugin - Remove next previous buttons
- Deployment sets - Remove next previous buttons not useful
- Deployment sets - Select target modal wrong title fix
- Apps sets creation/edition - Select applications show no apps message when no
  apps
- Apps sets creation - Sets and configuration tab not presented the same way may
  confuse user
- Apps sets creation/edition - Require user confirmation for delete app
- Apps sets creation - Click on apps link during creation trigger changes modal
  fix
- Apps - Provide a description template for applications sets and for kubernetes
- Apps - Subtype should not be visible when creating
- Apps - Disables paste/add buttons in projects with no edit rights
- Deps - Upgrade deps #1237
- Deployment details - Removes GUID from page header
- Apps - Adds hint for image format on logo modal
- Deployment - Bulk action modals only opening when enabled #1280
- Edge details - Node labels full width of row
- General - Design changes latest changes
- Deployment dialog - Do not consider execution mode saved in deployment
- General - Use new icons by creating icon components #1236
- Apps - remove more details collapsed section from details tab #1271
- Apps - Save button should become blue when enabled #1269
- Apps - Shows house icon as default in breadcrumbs
- Apps - Tab changes ignores changes protection
- Apps - Resets tab to default when changing apps
- Apps - Remove project from the app title #1235
- Apps - Do not show paste button on project creation page
- Invite - Error message missing space fix
- Deployment page - Support for bulk edit tags

## [2.33.7] - 2023-05-19

### Changed

- Deployment set - Accept licenses widget #1220
- Deployment set details - Show prices section and require user confirmation
- Deployment set details - Show licenses concerned applications
- Application sets - Link to apps
- Deployments page - bulk actions #1195
- Session - Switch group add divider between hierarchy #1231
- App - Navigation version of one app affect another one bugfix #1244
- App - Makes EULA link clickable
- App - Provides a description template when creating a new app or project #1194
- Apps details - Fix SixSq Apps EULA link
- About page - Replace release notes by software versions link #1218
- Clouds page - Fixes colours of state icon, bugfix #1223
- Enables bulk actions in table plugin - Moves deployments bulk action feature
  to table plugin #1180
- Edges page - Bulk updating tags
- Edges overview, table view - Enables bulk editing of tags #1194
- Edges details, overview tab - Adds node labels to Cluster status card #1207
- Github e2e - Cache NPM packages, use playwright container, optimize order
- Config.json - Update licenses
- Deployments overview, table view - Enables bulk editing of tags #1209
- About page - Updates logos and text, #1262
- App, new projects - Hide paste button, #1270
- App details - Show only app name without parent project in title, #1235
- Welcome page - Always show house icon as default in breadcrumbs, #1255
- App details, projects - Tab changes ignores changes protection, #1258
- App details - Resets active tab to default when changing apps, #1240

## [2.33.6] - 2023-04-28

### Changed

- About page - Terms and conditions link added
- Api - Resource list not cleaning when changing to a different tab, resulting
  in impossibility of searching #1191
- Api - Query params should not be kept when changing collection and browsing in
  history
- Api - Double navigation fired when clicking on id
- Sementic extensions - Link component stop propagation on click event
- Api detail - Browsing history show empty page even if resource is cached
- Applications sets details - Use explicit subtype instead of guessing from apps
  selected #1213
- Deployment set detail - For each applications sets select targets depending on
  subtype #2356

## [2.33.5] - 2023-04-24

### Changed

- Profile - Button add vendor email misalignment fix

## [2.33.4] - 2023-04-24

### Changed

- Notification subscriptions - Shows GiB unit for network in modal
- Api Page - Persists and loads all filter parameters
- App - Allow user to choose how to run a docker application
- App - Rename "Docker compose" accordion to "Compose file"
- Apps - Validation of compose file renamed and enhanced error message
- Deployment modal - Select credential bugfix
- About page - Remove personae item
- About page - Support for feature flags
- Tabs - New design
- Apps Store, Navigate apps tab - New design
- Apps Project Details, Overview tab - New design
- Apps Project Details, Details tab - New design
- Apps Project Details, Share tab - New design
- Apps Applications Details - New design, icons

## [2.33.3] - 2023-02-27

### Changed

- Deployment modal - Display infrastructure docker labels #996
- Deployment modal - Do not allow deployment of Swarm application on Swarm disabled node or worker node #996
- Deployment modal - Inform user that he is deploying a Compose application on a Docker swarm node #996
- Clouds and cloud details - Show compatibility label
- Logger - Add e2e test
- Sign in/up - Removes border from buttons, bugfix #1177
- Filter wizard - Additional filter is stored in and loads from query param
- Filter wizard - Reopening modal with active filter shows correct attribute and condition in dropdown, bugfix #1159

## [2.33.2] - 2023-02-27

### Changed

- Apps - Icon in modal should not be red
- CSS - Force blue color for all huge or massive icons within cards
- API - Filter modal should not open when user hit enter key bugfix #1136
- API - Disable select on navigation for collection selector
- API - Fire search when user select a collection
- API detail - Do not fire search when user is on detail page
- Routing - decoding redirect query param on sign-in page, fix #1133
- Edges overview - Saves current view as query param, #1125
- Edges overview - Stores to and loads preferred view from localStorage, #1141
- Favicon - Changes favicon to lighter red
- API detail - User can see raw json on api detail even when he is not able to edit #1145
- Global - Refactor CodeEditor with default-options
- Edge - Last and next telemetry report time ago refreshed #1140
- Edge detail - Last and next telemetry report time ago refreshed #1140
- API page - Make list of plain ids navigable #1143
- Pagination - Styling
- Apps AddModal - Disables docker and k8s apps buttons when not inside a project, regression fix #1150
- Full text search - Stores full text search in query param, #1128
- Edges overview - Stores and loads state filter from query param, #1127
- Deployments overview - Stores and loads state filter from query param, #1127
- Nav tab - Bugfix when panes content change, nav-tab should re-render bugfix #1158
- Apps - Tags alignment fix
- Components - Tags button border-radius fix
- Apps - Change logo button border-radius fix
- Deps - Codemirror v6 and replace react-codemirror2 by @uiw/react-codemirror

## [2.33.1] - 2023-01-30

### Changed

- Apps details - Deployments tab only applying external filters (= module id) for fetching deployments, bugfix #1112
- NuvlaEdge details - Deployments tab only applying external filters (= edge id) for fetching deployments, bugfix #1112
- Deployment Sets - Deployments table only applying external filters (= deployment sets id) for fetching deployments,
  bugfix #1112
- Jobs - Scrollable table
- Events - Scrollable table
- Global - Fix issues noticed after release #1117
- Api page - Move documentation button to menu and hide it on mobile
- Api details page - Pushes content below AclButton
- Switch group - Switch group on enter #1122
- Switch group - Change selection of group with arrow keys
- Switch group - Allow to search group including spaces
- Cimi detail - Display simple string ids as links or list of links when multiple ones #1120
- Global - Display parent resource as link
- Routing - Navigating to same page should not delete running actions intervals regression fix #1124
- Routing - Clicking link that leads to identical match should not add to history stack fix #1130

## [2.33.0] - 2023-01-26

### Changed

- Apps - App description truncated if bold text is used fix
- Values - Markdown summary should support nested elements #1077
- Deps - Upgrade api client v2.0.11
- Main - On error, main loader displays unavailable
- NuvlaEdge details - Unify layout for IPs and network interfaces, removes click to show anchor text #1051
- Edges overview - Adds more advanced filtering similar to e.g. deployments tab #1010
- Routing - Introduces reitit frontend router for client side routing
- Credentials - Add modal obfuscating password input fields #1084
- NuvlaEdge - Add and edit modals can handle nuvlabox-release endpoint sending back published flag #1053
- Tables - Enables horizontal scroll on tables, bugfix #1089
- NuvlaEdge Details - Makes changing tab a navigation via query params #1092
- Deployment Details - Makes changing tab a navigation via query params #1094
- Data page - Makes changing tab a navigation via query params #1099
- Credentials page - Makes changing tab a navigation via query params #1098
- Apps page - Makes changing tab a navigation via query params #1097
- e2e tests - Marketplace tests checks for param in request body #1095
- NuvlaEdge details - Shows pre-release flag behind version number #1054
- NuvlaEdge details - Optimize vulnerabilty
- NuvlaEdge details - Always shows tags card in nuvlaedge details overview, bugfix #1101
- Notification subscriptions - Addmodal: Disables greying out of unchecked radio button
- Notification subscriptions - Sort config always showing NuvlaEdge first
- Notification subscriptions - Shows configured tag for resource-filter in edit config modal
- Downloads - Rename and refactor nuvlabox-self-registration.py to nuvlaedge-self-registration.py
- Global - Adding translations
- Global - Styling

## [2.32.12] - 2023-01-06

### Changed

- Deployment modal - Check dct error not visible fix
- Application detail - Message become vendor bugfix
- Application detail - License renamed to End-User License Agreement
- Deployment modal - License renamed to End-User License Agreement
- Module plugin - License renamed to End-User License Agreement
- Paginations - Items per page is stored to and loaded from localStorage #1009
- Main - Remove deprecated message and bootstrap-message
- Readme - Update dev section to include skip verification of untrusted certificate
- Notifications - Added params to define time window reset and device name
- App store - Format price according to standards and locales #1048

## [2.32.11] - 2022-12-19

### Changed

- Dev tools - Integration of portal
- Deployment modal - update button disabled when registries is added in module newer version bugfix #1038
- Cloudflare preview deploys - Simulate behavior of nginx prod server
- Deployment - clone bugfix #1045
- CIMI-API - operation on-error default handler and signature change
- General - operation signature change alignment
- Table widget - adds re-usable tabled widget with wort functionality #1029
- Edges overview - Makes table sortable by column using table widget #1005
- Deployments overview - Makes table sortable by column using table widget #1005
- Deployment detail - Clone button should be disabled when operation not available
- Profile - User should confirm deletion of coupon or payment methods #1052
- Profile - Hide start getting paid when user is already a vendor
- Apps - Hide pulish and unpublish when not possible for the user
- Apps - Hide copy on menubar when application is not free and users doesn't have edit #1058
- Initialize - Load vendor on initialize and indirectly when switching group
- NuvlaEdges table view - Shows report interval #1013
- Deployment modal - Inform vendors and user with edit-right that it's free for them #1063
- Dependencies - Update to parent 6.7.12

## [2.32.10] - 2022-12-06

### Changed

- Profile - Payment method rename Bank account to SEPA
- Application - Bugfix license is highlighted in red even if not required nuvla/ui#1031
- Values - remove duplicated parse-ago
- Apps - Saving an app should move user to it #991
- Deployment set - Experimental feature (hidden for prod env)
- Time - delta minutes bugfix
- NuvleEdge details - Shows public and interface IPs in host card #969
- E2E nuvlaedge - fix tests to align with api changes
- NuvlaEdge details - Remove outline on the gages and display chart titles #988
- NuvlaEdge details - Remove ID column #987
- Logs view - Fix broken view of datepicker, bugfix #992
- NuvlaEdges - Table view shows ".y.z" for new edges version number #962
- Global share tabs - Show removing check mark only if owners editable, bugfix #1017
- NuvlaEdge details - Hides owner dropdown in share tab, bugfix #998
- NuvlaEdges table view - Table uses full width #1007
- NuvlaEdges and Deployments - Makes table view default #1012
- NuvlaEdges and Deployments - Sets default items per page to 25 with increasing items-per-page multiplier #1004
- NuvlaEdges table view - Makes pagination details more readable #1008
- NuvlaEdge details - Changes wording of menu item "Enable host level management" to show relation to playbooks #874
- Global filter wizard - Always validate input and disable Done button if invalid #982
- Global filter wizard - Sort values in filter wizard dropdown #981

## [2.32.9] - 2022-11-17

### Changed

- Testing - Added playwright for e2e testing UI and Server
- Global - upgrades react-datepicker to the newest version
- Global - upgrades react-charts-js to the newest version
- Global - replaces moment with date-fns
- CI - adds bundle analyzer job to pipeline
- NuvlaEdge details - Shows number of deployments in a bubble on tabs item #940
- NuvlaEdge details - Fixes opening Dropdown menu on key presses inside Update NuvlaEdge form Env Vars bugfix #908
- NuvlaEdge details - Shows currently installed modules in update modal as checkboxes #943
- NuvlaEdge details - Disables Save button on location tab if location unchanged, bugfix #949
- Apps - Replaces "App Store" with "Marketplace" #901
- Deployments - higher z-index on manual filter button to enable click on medium-sized screens, bugfix #950
- Global - Clears change protection event listener on before unload when Ignore Changes clicked, bugfix #948
- App details - Configuration Files section warns if Files are not supported instead of hiding #813
- Edges table view - Shows state icons again, bugfix #60
- Edges details - Resource consumption tab showing full container name in table #834
- Search plugin - Fixes jump of caret to back when typing in search input fields, bugfix #974
- Global - Renames all occurrences of "NuvlaEdge Engine" to "NuvlaEdge" #946

## [2.32.8] - 2022-10-14

### Changed

- Dictionary - Replace module by application, typo fix, NuvlaEdge/Cloud order change #916
- acl - Make possible add principals by user/group uuid
- acl - Principals dropdown searchable with groups/user
- Dev tools - Cloudflare preview integration
- NuvlaEdge overview - Shows last online times for offline edges #895
- NuvlaEdge overview - Shows engine versions on table view #909
- NuvlaEdge details - Fixes opening Dropdown menu on key presses inside Update NuvlaEdge form bugfix #908
- Deployments overview - Shows created by on table and list view #926
- Edges overview - Shows created by on table and list view #926
- Profile - Removes auto closing modal on tab change to enable e-mail 2FA bugfix #918
- Edges and Deployments - Adds filter indicator to full text search
- Filter comp - Show filter in popoup and color the button when filter is set

## [2.32.7] - 2022-09-29

### Changed

- Deployment detail - Page not refreshed bugfix #906
- Profile - Let user choose Sepa payment again

## [2.32.6] - 2022-09-28

### Changed

- Home - Update old links to point to new NuvlaEdge documentation
- Profile - Groups tabs warns user of unsaved changes
- Apps details - Adds button enabling re-opening comparison after closing it
- App store - Only shows published apps on initial load bugfix #900
- App store - My apps should filter on active-claim bugfix
- App store - Show published tick also in my apps tab bugfix
- App details - Pricing tab Follow user trial period toggle button not shown for read only app bugfix #897
- NuvlaEdge add modal - Copies correct value to clipboard for cronjob when host level management enabled bugfix #879
- NuvlaEdge add modal - Shows correct value for cronjob in hover popup bugfix
- Global - Adds cursor style pointer to copy to clipboard component bugfix

## [2.32.5] - 2022-09-19

### Changed

- Edge details - Reboot icon for operation in menubar
- Edge details - Hide create log action from menubar
- Edge details - Status segment color was depending on online status
- Edge details - Regroup and simplify status to end users
- Edge details add modal - Points more info links to new documentation
- Notifications - Renames "NB online" to "NuvlaEdge online"
- Session - ICRC sign-in and sign-up support
- Tab plugin - supports warning user of unsaved changes
- NuvlaEdge details - location tab warns user of unsaved changes
- Global - warn user when closing browser tab of unsaved changes
- NuvlaEdge - Envsubst of docker-compose files support NuvlaEdge renaming
- General utils - envsubst on string

## [2.32.4] - 2022-09-02

### Added

- Deployment fleets page (experimental hidden)
- Module versions plugin
- Step group plugin
- Full text search plugin
- Events plugin
- Tab plugin
- Pagination plugin
- Plugin concept introduction

### Changed

- Global - use plugins in all pages
- Deployment table fix query filter conflict
- Pagination - Enhance pagination out of range behavior
- Edge navigation to cluster break pagination of main page
- Deps - update re-frame
- i18n - Store tr in db to allow event error translation
- Deployment modal - Bugfix when credential of deployment doesn't exit a random
  infra is selected
- Deployment modal - Enhance update error message when credential or
  infrastructure doesn't exist and close modal

## [2.32.3] - 2022-08-25

### Changed

- Deployments - Move deployments tab to a new page

## [2.32.2] - 2022-08-03

### Added

- Assets - add email header image

### Changed

- Components - Bulk action progress wrong header and enhancements
- Deployments - Hide version when not resolvable without extra calls

## [2.32.1] - 2022-07-12

### Changed

- Deployments - Add version to deployment card and row
- Deployment detail - Remove credential and replace with unified cloud/nuvlaedge
  link
- Edge detail - Remove version from overview
- Deployment detail - Hide urls from overview when state is stopped and disable
  links in urls tab

## [2.32.0] - 2022-07-07

### Added

- Main - Modal and label to notify about UI release and propose user to reload

### Changed

- Create NuvlaEdge additional modules not respected when changing version
- NuvlaEdge detail - Create-log not created bugfix
- Switch group menu - auto-focus search field on open
- Switch group menu - search filter on close persisted but filtering is lost
  bugfix

## [2.31.2] - 2022-07-01

### Added

- Edge detail - Overview display "created by" when owner is a group
- Deployment detail - Overview display "created by" when owner is a group

### Changed

- Deployment detail - Rename module section renamed app.
- Deployment detail - Show owner of deployment or acl owners when no owner
- Profile - Creation subscription for root group not possible bugfix

## [2.31.0] - 2022-06-29

### Added

- Switch group menu - Display subgroups that the user is part of
- Switch group menu - Allow optionally to extend claims to all subgroups and
  represent groups hierarchy
- Switch group menu - Searchable
- Profile - Display customer email

### Changed

- Profile - Align with API coupon moved to subscription
- CIMI - Error getting resource-metadata/data-object fix
- Apps - My apps show application owned by the active-claim instead of connected
  user
- Project - id being truncated on details pages fix
- Apps - active-tab navigation bugfix
- Profile - add coupon modal displayed after a remove of coupon fix

## [2.30.4] - 2022-05-17

- Clouds - Page navigation bugfix
- Tab selection is based on keys for all places where it is used

## [2.30.3] - 2022-05-13

### Changed

- NuvlaEdge detail - Deployment list filtering regression bugfix
- NuvlaEdge detail - Deployment tab selection bugfix

## [2.30.2] - 2022-05-12

### Added

- Main - Alias support for URL page navigation

### Changed

- Profile - Hide Billing for subgroups and display them when customer is
  inherited from root group
- Profile - Show coupon end date for reapeating coupons
- Edge detail - Add services box
- Clouds - Remove NuvlaEdge from cloud list
- Project - Renamed NuvlaBox and infrastructure
- Project - Fix broken links to docs

## [2.30.1] - 2022-04-29

### Changed

- Project - update parent version

## [2.30.0] - 2022-04-29

### Changed

- Profile - Display customer balance
- Profile - Handle canceled subscription and reactivation of it
- Deps - Fix vulnerabilities
- Sign-in - Automatically redirect user when user want to access a protected
  page but doesn't have a session
- Sign-in - Support redirect query parameter to change page after a set session
- Notification - Set this page as protected
- Package.json - Fix scripts and js filename
- Deps - update to @stripe/react-stripe-js@1.7.0
- Deps - update to @stripe/stripe-js@1.24.0
- Profile - Warning logged to console fix because of list payment methods fix
- Sign-in-token - Warning logged to console fix
- App - Accordion missing in pricing section for component
- Pricing - support follow customer trial attribute

## [2.29.1] - 2022-03-09

### Changed

- ResponsiveMenuBar open when a user click into modal
  bugfix [#805](https://github.com/nuvla/ui/issues/805)

## [2.29.0] - 2022-03-07

### Added

- Component - CopyToClipboardDownload
- NuvlaBox detail page - add Logs tab
- Resource log reusable component
- Main component - ResponsiveMenuBar
- Session/Profile - Two factor authentication TOTP method support

### Changed

- NuvlaBox detail - Change download button in SSH modal
- NuvlaBox detail - Bugfix allow generation of SSH credential
- Credential - Show generated credential for SSH and API key
- Fixed "stop logs" action button for deployment and NuvlaBox logs
- Edge detail - Use ResponsiveMenuBar
- Cimi detail - Use ResponsiveMenuBar
- Cimi detail - Format operation simplified and transformed in a plain function
- NuvlaBox Cluster - [bug] edge page subscriptions are not refreshed on "
  navigate" [#630](https://github.com/nuvla/ui/issues/630)
- NuvlaBox Cluster - [bug] NB cards view disappears when in cluster
  view [#760](https://github.com/nuvla/ui/issues/760)
- NuvlaBox Cluster - [bug] cluster view listing weird
  NuvlaBoxes [#755](https://github.com/nuvla/ui/issues/755)
- NuvlaBox Cluster Detailed - print status notes cluster detailed view
- Footer - Make copyright year in footer update
  automatically [#790](https://github.com/nuvla/ui/issues/790)

## [2.28.0] - 2022-02-04

### Changed

- Profile - Warning in console fix for list of payment methods
- Main subs - is-mobile-device? new reframe subs
- Authn Menu - Remove documentation, support and invite user entries
- Authn Menu - Dropdown changed to MenuItems
- Nuvlabox detail page - hide generate-new-api-key
- Session - Error message in sign-in and sign-up pages not always
  visible [#778](https://github.com/nuvla/ui/issues/778)

## [2.27.2] - 2022-01-19

### Changed

- Data - on Data page, rename "Map filter" accordion to something simpler and
  more obvious [#770](https://github.com/nuvla/ui/issues/770)
- Data - setting nuvla-api parameter in config.json is not taken in to account
  [#774](https://github.com/nuvla/ui/issues/774)
- Sign-in-token - Warning logged to console fix

## [2.27.1] - 2022-01-17

### Changed

- Profile - Bugfix race condition loading customer and user

## [2.27.0] - 2022-01-14

### Added

- UIX - Translation component
- Profile - Enable or disable 2FA (Two factor authentication)
- NuvlaBox - new options for opting in on host-level management, at NuvlaBox
  creation time
- NuvlaBox - new tab for managing playbooks
- NuvlaBox - new buttons for playbook management operations

### Changed

- Error message parsing return in some cases false without any message fix
- Dependencies updates
- Map - Support draw
- Data - New workflow
- App application detail - change order tabs fix issue when no pricing is
  configured

## [2.26.2] - 2021-12-14

### Changed

- App store - Regression re-frame events logged to console
- Edge detail - Bugfixes in Location tab

## [2.26.1] - 2021-11-17

### Changed

- Module detail - Warning message appear twice when not on latest version fixed
- Edge detail - Clustering modal warn user on feature stability

## [2.26.0] - 2021-10-28

### Added

- Obfuscate input field for passwords and secrets
- Allow to set advertised address when forcing a new Swarm cluster on a NuvlaBox
- Edge detail - Do not propose execution-mode in add or revoke SSH credentials
  modal

## [2.25.0] - 2021-10-21

### Added

- Support for OpenStack infrastructures and credentials

### Changed

- Fix selection of default NuvlaBox version during creation
- Do not display hashed password type credentials in credentials page

## [2.24.0] - 2021-10-13

### Changed

- NuvlaBox update action has new option to force restart during the update
  operation

## [2.23.0] - 2021-10-13

### Changed

- Nuvlabox add modal doesn't select pre-release versions
- NuvlaBox map view now show all NuvlaBoxes, including NBs with inferred
  location
- Fix refresh of cluster info on default NuvlaBox view

## [2.22.0] - 2021-09-14

### Added

- Introduced a spinner when navigating through pages, providing a smoother user
  experience.
- Introduced on app store ability to change the number of items per page

### Changed

- Fix bug where dropdown in the share tab for NuvlaBox where trimmed
- Fix bug where VPN dropdown on New NuvlaBox is empty
- Fix bug with "Cluster actions" on NuvlaBox.

## [2.21.0] - 2021-08-31

### Changed

- Fix bug where dropdown in the share tab for modules, NuvlaBox and
  infrastructure services where trimmed
- Fix bug where apps selected from datasets caused an error
- Improved read only view of apps
- Fixed validation errors for apps
- Added tags to apps summary segment
- Improve name resolution for authors in app, deployment and NuvlaBox pages
- Provide more specific empty list message for deployments in apps and NuvlaBox
  pages
- Add tags to apps overview tab
- Improve layout of apps tabs and add deployment statistics to overview tab
- Improve on hover pop-up behaviour for tags
- Improved loading pages
- Added editing of NuvlaBox description
- Improve cluster view
- Improve data page

## [2.20.0] - 2021-08-04

### Added

- Apps with environment variables named "S3_CRED" or "GPG_CRED" automatically
  show a dropdown of available credentials of that type in the launch modal.
- Profile - Group tab
- Support for notification of creation of data-record, used for the new Blackbox
  application
- New layout of data-records and data-sets to better understand and view
  data-records

### Changed

- Bulk stop - filter bugfix
- Fixed local validation for creating new credentials (GPG and SSH)
- Fix fields validation for apps during editing
- Edge Detail - Improve warning and error messages on NB update
- Edge - Search icon under input fix
- Edge - Enhance responsiveness of statistics
- Edge - Regression fix, double anchor to show additional filters
- Updated SixSq legal status (Sàrl to SA)
- Acl widget - use peers instead of search user resource
- Session - get peers support

## [2.19.1] - 2021-06-24

### Changed

- Edge details - Bugfix docker plugins rendering causing blank page
- Session - Sort groups and remove prefix in authentication menu
- Improve contrast for selected item in sidebar menu
- Show not found message for NuvlaBox

## [2.19.0] - 2021-06-04

### Changed

- Main comp - Show job errors per action when some
- Credential - Support gpg key
- Main - Detect unpaid subscription to show a more coherent message
- Deployment - Bulk force delete support
- Deployment - Bulk stop support
- Fix visible console errors after update
- React-chart-2 - Fix broken changes
- React-leaflet - Fix broken changes
- NPM dependencies - Major update
- Dependencies - sixsq.nuvla/parent 6.7.5
- Credentials - Improve layout of logos for cloud providers

## [2.18.1] - 2021-05-19

### Added

- Show an error message when an element does not exist or is not accessible
  (modules, NuvlaBoxes and infrastructure services)
- Edge detail page - add container monitoring stats

### Changed

- Edge detail - Nuvlabox cluster action bugfix
- Deployment detail - Clear log bugfix
- Deployment dialog - Remove duplicated definition of price subscription
- Deployment detail - Markdownified module description
- Improved responsive display of module overview

## [2.18.0] - 2021-05-06

### Changed

- Sign-in / Sign-up - Hide text when no external templates configured
- Edge page - Clustering
- Clj-condo fix errors and warnings
- Module - Publish module
- Module - Refactor module page in tabs
- Authn menu - Sort groups on the switch group list
- Edge page - Creation of nuvlabox-installation-trigger-usb.nuvla doesn't set
  api-credentials ttl
- Edge page - Creation of nuvlabox-installation-trigger-usb.nuvla creation data
  content fix

## [2.17.2] - 2021-04-28

### Added

- Support for apps to ask for a credential with user permissions

### Changed

- Deployment modal - Bugfix icon credential check
- Deployment - Display credential name when available in deployment
- Sign-in - Make reset password link position absolute
- Sign-in - Support geant provider
- Sign-up - Support geant provider

## [2.17.1] - 2021-04-16

### Changed

- I18n - Bugfix prevent loop in setting tr function
- Acl widget - Bugfix default acl take into account active-claim
- Session - enhancement

## [2.17.0] - 2021-04-09

### Changed

- i18n - Load locale from storage
- Deps - re-frame-storage-fx
- Sticky bar - Bug fix z-index collision with open modal
- Main comp - Bulk modal progress
- Filter comp - Fix bug in date-time field
- Deployment - Bulk update
- CIMI API - Bulk operation support
- Deployment - Bugfix start logging create an infinite request loop when action
  not available
- Deployment - Additional filter
- Dependency - nuvla-api-version v2.0.9
- Sidebar item has an href
- Deployment page - Use card component
- Infrastructure page - Use card component
- Edge page - Use card component
- Edge detail page - Use tags component
- UIX - Reusable tags component
- UIX - Reusable card component
- Sidebar - Improve order of pages
- Deployment - Summary all for deployments
- Edge page - Improved statistics usage for NuvlaBoxes
- Dashboard - Deployments and NuvlaBox summary

## [2.16.0] - 2021-03-08

### Changed

- Edge page - Allow creation of Nuvlabox without VPN
- Notifications page - open first accordion, added refresh buttons, displaying
  number of actual subscriptions per configuration. Allow to define multiple
  notification methods per subscription configuraiton.

## [2.15.0] - 2021-02-22

### Changed

- Apps - ability to apply diff between two module versions
- Dependency - NPM react-diff-viewer package
- Deployment modal - Submit button show loading when clicked
- Menubar buttons - Capitalize all of them
- Modal headers - Capitalize all of them
- Format operation - some action names are lower case
- Deployment detail - Version behind fix
- Edge details - Update Nuvlabox modal support a payload
- Ocre - Decommission voucher resources
- Help message for notification destination.

## [2.14.1] - 2021-02-16

### Changed

- Fix: empty filter as default for components notification config.

## [2.14.0] - 2021-02-16

### Added

- Notifications tab with an ability to manage notification methods and
  notification subscriptions.
- Edge detail - dynamic lookup on the vulnerabilities DB to get more details on
  each NuvlaBox vulnerability

### Changed

- Fix/update broken external links
- Edge detail - Modal update warning message for old NBE

## [2.13.2] - 2021-02-10

### Changed

- Fulltext search enhanced
- ACL - Search users fix
- Edge detail - Specific modal for nuvlabox update operation
- Edge detail - Add modal fix bug in existing list of ssh keys without a name

## [2.13.1] - 2021-02-09

### Changed

- Edge detail - Hide raw edit, duplicated delete and activate operations from
  menubar

## [2.13.0] - 2021-02-09

### Added

- UIX - Message warning no elements to show
- Edge detail - New deployments tab
- Edge detail - New jobs tab and an error message is visible when last executed
  job is failed
- Edge detail - Specific modals for add ssh key and for revoke ssh key

### Changed

- Acl - Vertical align owner and dropdown
- Deployment - Overview tab, state transition loader moved
- Operation modal - When a specific modal exist show it instead of generic modal
- Cimi detail - operation support action operation body when metadata is defined
- Deployment modal - distinguish credential check error and only let deploy if
  credential is not invalid
- Edge - Move and simplification of NuvlaBox card component
- Edge detail - Use NuvlaBox new online attribute
- Edge - Use NuvlaBox new online attribute
- Credential - tab based design
- Deployment - tab based design
- Docs - Fetch documents when user goes directly to details page
- Profile - Send events to Intercom related to trial period end email
  notifications
- Deployment modal - On update credential can be changed
- Deployment modal - On update only selected infra is visible
- Tools - Re-frame-10x is disabled by default
- Static resources - update NB auto installer Python script

## [2.12.0] - 2020-12-10

### Changed

- Edge - fix SSH key assignment to NuvlaBox at creation time
- Credential views - Fix SSH visibility in credentials list
- Filter comp - Processing of resources metadata do not consider vector of map
  type fix (#499)
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
- Resource-metadata - Too big, optimization to get it only on api documentation
  page
- Sidebar - logo link is defined by config nuvla-logo-url
- Config - new nuvla-logo-url attribute
- Config - new pricing-url attribute
- Pricing - Remove pricing page
- Deployment - Click on error message make user go to job section
- NuvlaBox - add new icon for Bluetooth peripherals

## [2.9.0] - 2020-10-28

### Changed

- Deployment - Show last failed job on top
- Dashboard detail - renamed deployment
- Deps - Popper dependency fix
- Dashboard detail - version bug fix
- Shutdown modal - check credential
- Sign-up - Redirect to sign-in after sign-up to fix safari save password
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
- Nuvlabox detail - Extended view for peripheral list in NuvlaBox details page,
  to cope with new resource telemetry attributes

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
- Added support for file signature with GPG for nuvlabox-self-registration.py
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
- Infrastructure service modal augmented with creation of Docker Swarm or
  Kubernetes on cloud providers.

### Changed

- Apps - Edit of ports mapping, urls, env vars, output params, volumes, files
  and data-types inserted randomly when more than 8 values fix
- Dashboard - fix show empty URL in table view
- Config - new attribute stripe
- Sign-up - Integration with pricing feature
- Main - Modal subscription required
- NuvlaBox detail - Fix bug when saving location
- Danger-Modal - Fix shutdown deployment in some condition button had no effect
- Fix typos and text consistency
- Add instructions on how to enable fast deployment monitoring

## [2.4.15] - 2020-05-12

### Changed

- Container - exclude config.json and nuvla-logo.png from precompress mvn
  prepare-package

## [2.4.14] - 2020-05-11

### Added

- Dashboard detail - deplyment in stopped state are restartable and rename stop
  operation shutdown in UI
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
- Application - Module compatibility and docker-compose validation on get module
- Dependencies - udpate parent to v6.7.3
- Edge page – Create nuvlabox modal generate a single zip for NuvlaBox engine
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

- Data page / Deployment modal - replace data-records map by data-records-filter
- Component - New component time-ago
- Deployment modal - check credential of registries
- Module component - fix bug in edit with private registries
- DEPENDENCIES - update dependencies
- Deployment modal - support private registries
- Module component - Support private registries
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
- DEPENDENCIES - Fix version of closure-compiler-unshaded to be aligned with
  shadow-cljs version
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
- OCRE - Set default query params at page enter to remove influence from CIMI
  page
- CIMI PAGE - remove terms from statistic numbers since badly rendered
- DASHBOARD DETAIL - Support Kubernetes logs
- DASHBOARD DETAIL - Regression fix list of services for application not visible

## [2.4.7] - 2019-12-09

### Changed

- BOOTSTRAP_MESSAGE - Disable check for bootstarp message for swarm and creds
- DASHBOARD PAGE - Clickable cards better visibility
- DASHBOARD PAGE - Search Input
- Main components - Search input
- EDGE PAGE - Full text search
- Pagination - make pagination visible even if only one page is available
- EDGE PAGE - Replace dropdown filter by clickable icons
- CREDENTIAL - add vpn modal, default description and name is set for the user.
  Warning is displayed for the user to request user to save his generated
  credential
- CREDENTIAL - add modal select by default first infra if only one available
- OCRE PAGE - add pie chart and align schema with api-server
- CIMI PAGE - make name field visible by default
- DEPENDENCIES - Fix firefox callback issue in delete caused by bluebird version
  3.7.1
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
- Application detail - validate yaml syntax and print and show error and hints
  messages
- Dashboard detail - If not valid yaml blank page fix
- Edge details - Support nuvlabox-peripherals resource
- ACL - search users escape chars

## [2.4.5] - 2019-10-10

### Changed

- Infrastructure - Allow multiple instances of same service subtype in
  infrastructure group

## [2.4.4] - 2019-09-18

### Changed

- Dependencies - Update dependencies
- Edge - Add modal version moved to an advanced section
- Messages - Polling notifications enhance title and lifecycle management
- Favicon added
- Dashboard detail - fix concatenate log duplicates and rename scroll down to go
  live

## [2.4.3] - 2019-09-04

### Add

- Dashboard detail - log accordion
- Yaml parser dependency
- Component - TableRowField

### Changed

- Apps - Environmental variable align spec with server #230
- Dashboard detail - remove transparency to make search dialog visible and
  minors
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

- Authn - Support Github external authentication, API-SERVER version >=4.2.2
  required to work properly
- Welcome - Show error message coded in url sent by the server
- SpanBlockJustified - Component to display long description cleanly

### Changed

- Dependencies - Update shadow-cljs to version 2.8.42 and clojure-library to
  version 2.0.3
- Footer - Visual changes made
- Side bar - Visual changes made
- Api page - Changed to be a non protected page
- Side bar - Enhance detection of selected item #106 and click on protected
  pages open login page when no session #212
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

- Apps application - Remove fold-gutters from Docker-compose editor resolve
  visiblity issue
- Reuse subs apps, deployment-modals, authn
- Apps - Fix issue in controlled env variables
- Deployment modal - fix bug in eddition of env varialbes
- Page Header - Reuse page header

## [2.4.0] - 2019-07-24

### Changed

- Deployment modal - deployment should not get mounts fields when no distinct
  mounts
- Apps pages - Reuse editable as a subscription and remove duplicated code
- Cimi detail - Fix ACL not visible and make it editable
- Deployment modal - Ensure that a credential is set in deployment before start
  action

### Added

- Application/Module - support deployement of application with docker-compose
  and files

## [2.3.0] - 2019-07-11

### Changed

- App Component - Server conflict now on same path, notify user to choose
  another name #108
- App Component - Unclear how to fill docker image fields #96 #118
  # 199
- Deployment detail - add a section for "Environment Variables" #183
- Deployment - Harmonize cards between deployment details and deployment page
  # 185
- Deployment detail - add a section for "URLs" #184
- Deployment detail - clicking on an event or job link shows spinner (forever)
  # 179
- App Component - fix Validation error remains when deleting a component #196
- App Component - empty env values are allowed #186
- Apps - old search is applied even if search field is empty
- Upgrade to parent 6.5.1, nuvla api 2.0.1, shadow-cljs 2.8.39
- App component - Creating a new component sees previously used env vars #175
- ACL Button - ACL Button hide itself when no acl and in read-only mode #173
- Apps component - architectures should be separated in read-only mode #174
- Infrastructure page - add on service group allows creation of more than one
  service of the same type fix
- Login - disable login for password and api-key when not all required fields
  are complete
- Infrastructure page - take into account acl at creation
- Credential page - take into account acl at creation
- ACL - refactor to get ui-acl format to be able to keep order
- About page - Links update

## [2.2.0] - 2019-06-20

### Added

- Set document title when navigating to simplify history navigation
- Main components - Refresh Menu is now reusable and generalize it to all pages
- Action intervals - countdown feature and adding it on all automatically
  refreshed pages

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
- ACLs - Automatically add new rights when user select a principal and a right
  in new permissions row
- Apps - Project save should not show commit message and cancelling a commit
  message when saving a component should not remove is-new? flag
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
- Signup - Submit button is disabled and form cleared after a successful submit
- Create user - email of invited user is prefilled
- Session and User templates based modals display clear validation and human
  readable errors
- Deployment page - all urls are visible in deployment details and module link
  is present in details
- Deployment modal - support env variables
- Apps page - add env variables to module component
- Deployment owners replaced by metadata widget
- Metadata widget - doesn't show acl when acl is null
- ACL widget - indeterminate state in simple mode
- Dashboard page - URL in deployment card url is only visible when deployment
  state is started. Rename namespace to dashboard
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
