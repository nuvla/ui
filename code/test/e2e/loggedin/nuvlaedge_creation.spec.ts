import { test, expect } from '@playwright/test';

test('Creating an Edge with an older version',  async ({ page, context }, { project, config })  => {

  const { baseURL } = config.projects[0].use;

  await page.goto(baseURL + '/ui/welcome');

  await page.route('api/nuvlabox-release', async (route) => {
        route.fulfill({ status: 200, body: JSON.stringify(NuvlaEdgeRelease())});
      });
  await page.getByRole('link', { name: 'Edges' }).click();

  const newEdgeName = 'NE with older release';

  const edgesPageRegex = /\/ui\/edges/;

  await expect(page).toHaveURL(edgesPageRegex);

  await page.getByText('Add', { exact: true }).click();

  await page.locator('input[type="input"]').click();

  await page.locator('input[type="input"]').fill(newEdgeName);
  // We add the security module
  await page.getByText('security').click();

  await page.getByText('bluetooth').click();

  await page.getByText('gpu').click();

  await page.getByText('modbus').click();

  await page.locator('div[role="listbox"]:has-text("2.4.3") i').click();
  // We switch to an older version, for which the security module is not available
  await page.getByRole('option', { name: '1.11.0' }).click();

  await page.getByText('gpu').click();

  await page.locator('form > div:nth-child(2) > div').first().click();

  await page.getByText('Compose file bundle').click();

  await page.getByRole('button', { name: 'create' }).click();

  await page.getByRole('button', { name: 'close' }).click();
  // We verify that nothing has blown up, we get to see the edges table
  await expect(page.getByRole('link', { name: /select row 0/i })).toBeVisible();

});

test.afterAll(async ({ page, context }, { project, config }) => {

  const { baseURL } = config.projects[0].use;


  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'Edges' }).click();

  const edgesPageRegex = /\/ui\/edges/;

  await expect(page).toHaveURL(edgesPageRegex);
  await page.getByText('25 per page').click();
  await page.getByRole('option', { name: '100' }).getByText('100').click();

  const newEdgeName = 'NE with older release';
  await page.getByPlaceholder('Search ...').click();
  page.getByPlaceholder('Search ...').fill(newEdgeName);
  await page.waitForResponse('/api/nuvlabox');
  await page.waitForTimeout(1000);




  let found = await page.getByRole('link', { name: new RegExp(newEdgeName) }).count();
    while (found > 0) {
      await page
        .getByRole('link', { name: new RegExp(newEdgeName) })
        .nth(0)
        .click({ timeout: 5000 });
      await page.locator('a:has-text("delete")').click();
      await page.getByRole('button', { name: 'Delete NuvlaEdge' }).click();
      await page.getByRole('button', { name: 'Yes: Delete NuvlaEdge' }).click();
      await page.waitForURL(`${baseURL}/ui/edges?view=table`);
      await page.getByPlaceholder('Search ...').click();
      page.getByPlaceholder('Search ...').fill(newEdgeName);
      await page.waitForResponse('/api/nuvlabox');
      await page.waitForTimeout(500);
      found = await page.getByRole('link', { name: new RegExp(newEdgeName) }).count();
  }

});


const NuvlaEdgeRelease = () => (
{
  "count" : 62,
  "acl" : {
    "query" : [ "group/nuvla-user" ]
  },
  "resource-type" : "nuvlabox-release-collection",
  "id" : "nuvlabox-release",
  "resources" : [ {
    "release-date" : "2022-10-27T12:23:14Z",
    "updated" : "2023-02-28T23:19:17.454Z",
    "release" : "2.4.3",
    "created" : "2022-10-27T12:36:05.600Z",
    "pre-release" : false,
    "compose-files" : [ {
      "name" : "docker-compose.bluetooth.yml",
      "scope" : "bluetooth",
      "file" : ""
    }, {
      "name" : "docker-compose.gpu.yml",
      "scope" : "gpu",
      "file" : ""
    }, {
      "name" : "docker-compose.modbus.yml",
      "scope" : "modbus",
      "file" : ""
    }, {
      "name" : "docker-compose.network.yml",
      "scope" : "network",
      "file" : ""
    }, {
      "name" : "docker-compose.security.yml",
      "scope" : "security",
      "file" : ""
    }, {
      "name" : "docker-compose.usb.yml",
      "scope" : "usb",
      "file" : ""
    }, {
      "name" : "docker-compose.yml",
      "scope" : "",
      "file" : ""
    }, {
      "name" : "install.sh",
      "scope" : "",
      "file" : ""
    } ],
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
  }, {
    "release-date" : "2020-11-02T13:37:24Z",
    "updated" : "2023-02-28T23:23:42.109Z",
    "release" : "1.11.0",
    "created" : "2020-12-03T12:28:46.058Z",
    "pre-release" : false,
    "compose-files" : [ {
      "name" : "docker-compose.gpu.yml",
      "scope" : "gpu",
      "file" : ""
    }, {
      "name" : "docker-compose.modbus.yml",
      "scope" : "modbus",
      "file" : ""
    }, {
      "name" : "docker-compose.usb.yml",
      "scope" : "usb",
      "file" : ""
    }, {
      "name" : "docker-compose.yml",
      "scope" : "",
      "file" : ""
    }, {
      "name" : "install.sh",
      "scope" : "",
      "file" : ""
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
    "published" : false
  } ],
  "operations" : [ {
    "rel" : "add",
    "href" : "nuvlabox-release"
  }, {
    "rel" : "bulk-delete",
    "href" : "nuvlabox-release"
  } ]
});