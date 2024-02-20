import { test, expect } from '@playwright/test';

test('test',  async ({ page, context }, { project, config })  => {

   const { baseURL } = config.projects[0].use;
    await page.goto(baseURL + '/ui/welcome');
    await page.route('api/nuvlabox-release', async (route) => {
        route.fulfill({ status: 200, body: JSON.stringify(NuvlaEdgeRelease())});
      });
    await page.getByRole('link', { name: 'Edges' }).click();

  const edgesPageRegex = /\/ui\/edges/;

  await expect(page).toHaveURL(edgesPageRegex);

  await page.getByText('Add', { exact: true }).click();

  await page.locator('input[type="input"]').click();

  await page.locator('input[type="input"]').fill('Old release NE');

  await page.getByText('bluetooth').click();

  await page.getByText('gpu').click();

  await page.getByText('modbus').click();

  await page.locator('div[role="listbox"]:has-text("2.4.3") i').click();

  await page.getByRole('option', { name: '1.11.0' }).click();

  await page.getByText('security').click();

  await page.getByText('gpu').click();

  await page.locator('form > div:nth-child(2) > div').first().click();

  await page.getByText('Compose file bundle').click();

  await page.getByRole('button', { name: 'create' }).click();

  await page.getByRole('button', { name: 'close' }).click();

  await page.getByRole('link', { name: 'select row 0 Old release NE 2.y.z' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edges/69b85345-58c8-44a6-b1c4-427a0137414a');

});




const NuvlaEdgeRelease = () => ({
                                   "count" : 62,
                                   "acl" : {
                                     "query" : [ "group/nuvla-user" ]
                                   },
                                   "resource-type" : "nuvlabox-release-collection",
                                   "id" : "nuvlabox-release",
                                   "resources" : [ {
                                     "release-date" : "2022-11-17T09:29:58Z",
                                     "updated" : "2023-02-28T23:12:12.559Z",
                                     "release" : "2.4.4",
                                     "created" : "2022-11-17T13:52:09.373Z",
                                     "pre-release" : true,
                                     "compose-files" : [ {
                                       "name" : "docker-compose.bluetooth.yml",
                                       "scope" : "bluetooth"
                                     }, {
                                       "name" : "docker-compose.gpu.yml",
                                       "scope" : "gpu"
                                     }, {
                                       "name" : "docker-compose.modbus.yml",
                                       "scope" : "modbus"
                                     }, {
                                       "name" : "docker-compose.network.yml",
                                       "scope" : "network"

                                     }, {
                                       "name" : "docker-compose.security.yml",
                                       "scope" : "security"
                                     }, {
                                       "name" : "docker-compose.usb.yml",
                                       "scope" : "usb"
                                     }, {
                                       "name" : "docker-compose.yml",
                                       "scope" : ""

                                     }, {
                                       "name" : "install.sh",
                                       "scope" : ""

                                     } ],
                                     "updated-by" : "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
                                     "created-by" : "group/nuvla-admin",
                                     "id" : "nuvlabox-release/dd9ea193-9e90-4457-9cb4-a5ce45b1ea1b",
                                     "url" : "https://github.com/nuvlaedge/deployment/releases/tag/2.4.4",
                                     "resource-type" : "nuvlabox-release",
                                     "acl" : {
                                       "view-meta" : [ "group/nuvla-user" ],
                                       "view-data" : [ "group/nuvla-user" ],
                                       "owners" : [ "group/nuvla-admin" ]
                                     },
                                     "operations" : [ {
                                       "rel" : "edit",
                                       "href" : "nuvlabox-release/dd9ea193-9e90-4457-9cb4-a5ce45b1ea1b"
                                     }, {
                                       "rel" : "delete",
                                       "href" : "nuvlabox-release/dd9ea193-9e90-4457-9cb4-a5ce45b1ea1b"
                                     } ],
                                     "published" : false
                                   }, {
                                     "release-date" : "2022-10-27T12:23:14Z",
                                     "updated" : "2023-02-28T23:19:17.454Z",
                                     "release" : "2.4.3",
                                     "created" : "2022-10-27T12:36:05.600Z",
                                     "pre-release" : false,
                                     "compose-files" : [ {
                                       "name" : "docker-compose.bluetooth.yml",
                                       "scope" : "bluetooth",
                                       "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=bluetooth\"\n\nservices:\n  peripheral-manager-bluetooth:\n    <<: *common\n    image: nuvlabox/peripheral-manager-bluetooth:1.2.0\n    restart: on-failure\n    network_mode: host\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - agent\n"
                                     }, {
                                       "name" : "docker-compose.gpu.yml",
                                       "scope" : "gpu",
                                       "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=gpu\"\n\nservices:\n  peripheral-manager-gpu:\n    <<: *common\n    image: nuvlabox/peripheral-manager-gpu:1.1.0\n    restart: on-failure\n    volumes:\n      - /etc/:/etcfs/:ro\n      - /var/run/docker.sock:/var/run/docker.sock:ro\n      - /usr/lib/:/usr/lib/:ro\n      - /dev/:/dev/:ro\n    depends_on:\n      - agent\n"
                                     }, {
                                       "name" : "docker-compose.modbus.yml",
                                       "scope" : "modbus",
                                       "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=modbus\"\n\nservices:\n  peripheral-manager-modbus:\n    <<: *common\n    image: nuvlabox/peripheral-manager-modbus:1.3.0\n    restart: on-failure\n    depends_on:\n      - agent\n"
                                     }],
                                     "updated-by" : "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
                                     "created-by" : "group/nuvla-admin",
                                     "id" : "nuvlabox-release/5dbe5376-955d-4bc4-a1b2-8043d4db8e82",
                                     "url" : "https://github.com/nuvlaedge/deployment/releases/tag/2.4.3",
                                     "resource-type" : "nuvlabox-release",
                                     "acl" : {
                                       "view-meta" : [ "group/nuvla-user" ],
                                       "view-data" : [ "group/nuvla-user" ],
                                       "owners" : [ "group/nuvla-admin" ]
                                     },
                                     "operations" : [ {
                                       "rel" : "edit",
                                       "href" : "nuvlabox-release/5dbe5376-955d-4bc4-a1b2-8043d4db8e82"
                                     }, {
                                       "rel" : "delete",
                                       "href" : "nuvlabox-release/5dbe5376-955d-4bc4-a1b2-8043d4db8e82"
                                     } ],
                                     "published" : false,
                                     "release-notes" : "## Commits\n- c9c908f: Update agent to 2.8.4 and job-engine-lite to 3.2.2 (schaubl)\n- 02ab680: Update NuvlaBox Engine version to 2.4.3 (schaubl)"
                                   }, {
                                     "release-date" : "2020-11-02T13:37:24Z",
                                     "updated" : "2023-02-28T23:23:42.109Z",
                                     "release" : "1.11.0",
                                     "created" : "2020-12-03T12:28:46.058Z",
                                     "pre-release" : false,
                                     "compose-files" : [ {
                                       "name" : "docker-compose.gpu.yml",
                                       "scope" : "gpu",
                                       "file" : "version: \"3.6\"\n\nx-common: &common\n  stop_grace_period: 4s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=gpu\"\n\nservices:\n  peripheral-manager-gpu:\n    <<: *common\n    image: nuvlabox/peripheral-manager-gpu:0.0.1\n    restart: on-failure\n    volumes:\n      - /etc:/etc:ro\n      - /var/run/docker.sock:/var/run/docker.sock:ro\n"
                                     } ],
                                     "updated-by" : "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
                                     "created-by" : "group/nuvla-admin",
                                     "id" : "nuvlabox-release/228ddb9e-d09b-48d0-9999-d53e9ea6d72e",
                                     "url" : "https://github.com/nuvlabox/deployment/releases/tag/1.11.0",
                                     "resource-type" : "nuvlabox-release",
                                     "acl" : {
                                       "view-meta" : [ "group/nuvla-user" ],
                                       "view-data" : [ "group/nuvla-user" ],
                                       "owners" : [ "group/nuvla-admin" ]
                                     },
                                     "operations" : [ {
                                       "rel" : "edit",
                                       "href" : "nuvlabox-release/228ddb9e-d09b-48d0-9999-d53e9ea6d72e"
                                     }, {
                                       "rel" : "delete",
                                       "href" : "nuvlabox-release/228ddb9e-d09b-48d0-9999-d53e9ea6d72e"
                                     } ],
                                     "published" : false,
                                     "release-notes" : "## Commits\r\n- [[3f30989](https://github.com/nuvlabox/deployment/commit/3f309896bc9ab1f51ac2749159fceb4d4714021f)]: upgrade agent - add automatic IP-based geolocation discovery (Cristovao Cordeiro)\r\n- [[274a6d4](https://github.com/nuvlabox/deployment/commit/274a6d46d8364d89520db1d3b14cce391a28590c)]: rename env var (Cristovao Cordeiro)\r\n- [[58514f5](https://github.com/nuvlabox/deployment/commit/58514f5eb38b8eacc35f93fbc2dcd86a7b30aaf8)]: add new env vars (Cristovao Cordeiro)\r\n- [[4ca7e3d](https://github.com/nuvlabox/deployment/commit/4ca7e3dd0b3ddb2a2a800ebde2557c87320cb938)]: add vulnerability scanning and support for vulnerability reporting (Cristovao Cordeiro)\r\n- [[1c5af37](https://github.com/nuvlabox/deployment/commit/1c5af37e3cb70c5f396c0385ed8439369bc59769)]: Update NuvlaBox Engine version in Compose file, to 1.11.0 (Cristovao Cordeiro)"
                                   } ],
                                   "operations" : [ {
                                     "rel" : "add",
                                     "href" : "nuvlabox-release"
                                   }, {
                                     "rel" : "bulk-delete",
                                     "href" : "nuvlabox-release"
                                   } ]
                                 });