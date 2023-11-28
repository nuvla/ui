import { test, expect } from '@playwright/test';

test('test', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;

  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'deployments' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployments');

  await page.getByRole('link', { name: 'Deployment Groups' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups');

  await page.pause();

  await page.getByText('Add').first().click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/create`));

  await page.getByRole('row', { name: 'Name' }).locator('i').click();

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').click();

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').fill('Depl Group Test 01');

  await page.getByRole('row', { name: 'Name' }).getByRole('button').click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/create`));

  await page.locator('main:has-text("Oops can\'t find deployment groupDeployment group does not exist or you do not ha") button').first().click();

  await page.getByRole('link', { name: 'BlackBox This app allows users to trigger the creation of an airplane… Project: sixsq Vendor: Vendorgroup/sixsq-vendor Price: free trial and then €0.33/day blackbox ready Add to selection' }).getByRole('button', { name: 'Add to selection' }).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/create`));

  await page.locator('main:has-text("Oops can\'t find deployment groupDeployment group does not exist or you do not ha") button').nth(2).click();

  await page.getByRole('link', { name: 'select row 0 e2e-Test-Do_not_delete NEW endtoend@sixsq.com' }).click();

  await page.getByRole('button', { name: 'Add to deployment group' }).click();

  await page.locator('a:has-text("Save")').click();
  let depSetUrlRegExp = new RegExp(`${baseURL}/ui/deployment-set/([0-9a-f-]*)\?.*`);
  await expect(page).toHaveURL(depSetUrlRegExp);
  let depGroupUuid = page.url().match(depSetUrlRegExp)[1];
  await expect(page.url().match(depSetUrlRegExp)).toBe('hi!');
  await expect(depGroupUuid).toBe('hi!');
  await page.pause();

  await page.getByText('Pending: 1 deployments to add').click();

  await page.getByRole('link', { name: 'BlackBox' }).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/`));
  // baseURL + '/ui/deployment-set/b86c4679-f41f-42ca-b7b6-7fb8801b07e9?deployment-groups-detail-views-apps-config=configure-set-0-app-module/5aa83244-5e03-4d30-adc5-6b9cf5db1e5c&deployment-groups-detail-tab=apps');

  await page.getByRole('link', { name: 'BlackBox' }).click();

  await page.getByRole('link', { name: 'Overview' }).click();
  // await expect(page).toHaveURL(baseURL + '/ui/deployment-set/b86c4679-f41f-42ca-b7b6-7fb8801b07e9?deployment-groups-detail-views-apps-config=configure-set-0-app-module/5aa83244-5e03-4d30-adc5-6b9cf5db1e5c&deployment-groups-detail-tab=overview');

  await page.locator('.value').first().click();
  // await expect(page).toHaveURL(baseURL + '/ui/deployment-set/b86c4679-f41f-42ca-b7b6-7fb8801b07e9?deployment-groups-detail-views-apps-config=configure-set-0-app-module/5aa83244-5e03-4d30-adc5-6b9cf5db1e5c&deployment-groups-detail-tab=overview&deployment-sets-detail-tab=edges');

  await page.locator('main:has-text("Oops can\'t find deployment groupDeployment group does not exist or you do not ha")').getByRole('link', { name: 'Edges' }).click();
  // await expect(page).toHaveURL(baseURL + '/ui/deployment-set/b86c4679-f41f-42ca-b7b6-7fb8801b07e9?deployment-groups-detail-views-apps-config=configure-set-0-app-module/5aa83244-5e03-4d30-adc5-6b9cf5db1e5c&deployment-groups-detail-tab=edges&deployment-sets-detail-tab=edges');

  await page.getByRole('link', { name: 'select row 0 e2e-Test-Do_not_delete 2.y.z' }).click();
  // await expect(page).toHaveURL(baseURL + '/ui/edges/15339469-0b4c-4c45-a0b7-65c494e99a3b');

  // await page.goto(baseURL + '/ui/deployment-set/b86c4679-f41f-42ca-b7b6-7fb8801b07e9?deployment-groups-detail-views-apps-config=configure-set-0-app-module/5aa83244-5e03-4d30-adc5-6b9cf5db1e5c&deployment-groups-detail-tab=edges&deployment-sets-detail-tab=edges');

  await page.getByRole('link', { name: 'Deployments' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-set/b86c4679-f41f-42ca-b7b6-7fb8801b07e9?deployment-groups-detail-views-apps-config=configure-set-0-app-module/5aa83244-5e03-4d30-adc5-6b9cf5db1e5c&deployment-groups-detail-tab=deployments&deployment-sets-detail-tab=edges');

  await page.getByText('There are currently no deployments running for this app.').click();

  await page.getByRole('link', { name: 'Overview' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-set/b86c4679-f41f-42ca-b7b6-7fb8801b07e9?deployment-groups-detail-views-apps-config=configure-set-0-app-module/5aa83244-5e03-4d30-adc5-6b9cf5db1e5c&deployment-groups-detail-tab=overview&deployment-sets-detail-tab=edges');

  await page.locator('div[role="listbox"]:has-text("CancelDelete")').click();

  await page.getByText('Delete').click();

  await page.getByText('There are currently no running deployments: you can safely delete this deploymen').click();

  await page.getByRole('button', { name: 'Delete' }).click();

  await page.getByRole('button', { name: 'Yes: Delete' }).click();

  await page.goto(baseURL + '/ui/deployment-groups');

  await page.getByRole('link', { name: 'Test Depl Group created 4 minutes ago NEW' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups/69f3bc92-1f6a-491b-9d8a-1cb8a4a17195');

  await page.locator('div[role="listbox"]:has-text("CancelDelete") i').first().click();

  await page.getByText('Delete').click();

  await page.getByText('There are currently no running deployments: you can safely delete this deploymen').click();

  await page.getByRole('button', { name: 'Delete' }).click();

  await page.getByRole('button', { name: 'Yes: Delete' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups');

  await page.locator('main:has-text("DeploymentsDeployment GroupsAddautomatic refresh in 7sRefresh0TOTAL0NEW0STARTED0") button').click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-set/create?sixsq.nuvla.ui.deployment-sets.subs/creation-temp-id-key=129742f5-0aa1-47e3-9d49-20cece533915');

});