import { test, expect } from '@playwright/test';

test('test', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;

  const testDeplGroupName = 'Depl Group ' + Math.random().toString().substring(2, 5) + ' - Test should delete me!'

  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'deployments' }).click();

  await expect(page).toHaveURL(baseURL + '/ui/deployments');

  await page.getByRole('link', { name: 'Deployment Groups' }).click();

  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups');

  await page.locator("a[aria-label='add']").first().click();

  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-groups/create`));

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').click();

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').fill(testDeplGroupName);

  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-groups/create`));

  await page.locator('.nuvla-apps button.add-button').first().click();

  await page.getByRole('link', { name: 'BlackBox This app allows' }).getByLabel('Add to selection').click();

  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-groups/create`));

  await page.locator('.nuvla-edges button.add-button').click();

  await page.getByPlaceholder('Search...').fill('e2e test do not delete');

  await page.getByRole('link', { name: 'select row 0 e2e-Test-Do_not_delete NEW endtoend@sixsq.com' }).click();

  await page.getByRole('button', { name: 'Add to deployment group' }).click();

  await expect(page.getByRole('button', { name: 'Add to deployment group' })).toBeHidden();

  await page.locator('a:has-text("Save")').click();

  const depSetUrlRegExp = new RegExp(`${baseURL}/ui/deployment-groups/([0-9a-f-]{20,})`.replace(/[/.]/g, '\\$&') + '(\\?.+)?');
  await page.waitForURL(depSetUrlRegExp);
  const depGroupUuid = page.url().match(depSetUrlRegExp)![1];
  await page.getByText('Divergence: 1 deployments to add').click();

  await page.getByRole('link', { name: 'BlackBox' }).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/deployment-groups/`));

  await page.getByRole('link', { name: 'BlackBox' }).click();

  await page.getByRole('link', { name: 'Overview' }).click();

  await page.locator('.nuvla-edges div').first().getByRole('button').first().click();

  await page.getByRole('link', { name: 'Edges' }).first().click();

  await page.locator('table tbody').getByRole('link').first().click();

  await page.getByRole('link', { name: 'Deployments' }).first().click();

  await expect(page).toHaveURL(baseURL + '/ui/deployments');

  await page.getByRole('link', { name: 'Deployment Groups' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups');

  await page.getByRole('cell', { name: testDeplGroupName }).first().click();

  await expect(page).toHaveURL(baseURL + '/ui/deployment-groups/' + depGroupUuid);

  await page.locator('div[role="listbox"]:has-text("Delete")').click();

  await page.getByText('Delete', { exact: true }).first().click();

  await page.getByText('There are currently no running deployments: you can safely delete this deployment').click();

  await page.getByRole('button', { name: 'Delete' }).click();

  await page.getByRole('button', { name: 'Yes: Delete' }).click();

  await page.waitForURL(baseURL + '/ui/deployment-groups');
});
