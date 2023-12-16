import { test, expect } from '@playwright/test';

test('test', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;

  const testDeplGroupName = 'Depl Group ' + Math.random().toString().substr(2, 5) + ' - Test should delete me!'

  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'deployments' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployments');

  await page.getByRole('link', { name: 'Deployment Groups' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups');

  await page.getByText('Add').first().click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/create`));

  await page.getByRole('row', { name: 'Name' }).locator('i').click();

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').click();

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').fill(testDeplGroupName);

  await page.getByRole('row', { name: 'Name' }).getByRole('button').click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/create`));

  await page.locator('.nuvla-apps button.add-button').first().click();

  await page.getByRole('link', { name: 'BlackBox This app allows users to trigger the creation of an airplane… Project: sixsq Vendor: Vendorgroup/sixsq-vendor Price: free trial and then €0.33/day blackbox ready Add to selection' }).getByRole('button', { name: 'Add to selection' }).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/create`));

  await page.locator('.nuvla-edges button.add-button').click();

  await page.getByRole('link', { name: 'select row 0 e2e-Test-Do_not_delete NEW endtoend@sixsq.com' }).click();

  await page.getByRole('button', { name: 'Add to deployment group' }).click();

  await page.locator('a:has-text("Save")').click();

  const depSetUrlRegExp = new RegExp(`${baseURL}/ui/deployment-set/([0-9a-f-]{20,})`
    .replaceAll('/', '\\/').replaceAll('.', '\\.') + '(\\?.+)?');
  await page.waitForURL(depSetUrlRegExp);
  const depGroupUuid = page.url().match(depSetUrlRegExp)[1];
  await page.getByText('Divergence: 1 deployments to add').click();

  await page.getByRole('link', { name: 'BlackBox' }).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-set/`));

  await page.getByRole('link', { name: 'BlackBox' }).click();

  await page.getByRole('link', { name: 'Overview' }).click();

  await page.locator('.nuvla-edges div').first().getByRole('button').first().click();

  await page.getByRole('link', { name: 'Edges' }).first().click();

  await page.locator('table tbody').getByRole('link').first().click();

  await page.getByRole('link', { name: 'Deployments' }).first().click();
  await expect(page).toHaveURL(baseURL + '/ui/deployments');

  await page.getByRole('link', { name: 'Deployment Groups' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups');

  //await page.getByRole('link', { name: testDeplGroupName }).first().click();
  await page.locator('a.ui.card[href="/ui/deployment-groups/' + depGroupUuid + '"]').click();

  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups/' + depGroupUuid);

  await page.locator('div[role="listbox"]:has-text("Delete")').click();

  await page.getByText('Delete', { exact: true }).first().click();

  await page.getByText('There are currently no running deployments: you can safely delete this deployment').click();

  await page.getByRole('button', { name: 'Delete' }).click();

  await page.getByRole('button', { name: 'Yes: Delete' }).click();

  await page.waitForURL(baseURL + '/ui/deployment-groups');
});
