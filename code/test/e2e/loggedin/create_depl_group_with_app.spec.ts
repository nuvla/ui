import { test, expect } from '@playwright/test';

let random_id = Math.random().toString(36).substring(2,7);

 test('create deployment group containing an app from deployment modal', async ({ page }, {config}) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'apps' }).click();

  await page.getByRole('link', { name: 'All Apps' }).click();

  await page.getByPlaceholder('Search...').click();

  await page.getByPlaceholder('Search...').fill('nginx');

  await page.waitForURL(`${baseURL}/ui/apps?apps-store-tab=allapps&apps-store-modules-search=nginx`);

  await page.getByRole('link', { name: 'Nginx Nginx (pronounced "engine-x") is an open source reverse proxy server for HTTP, HTTPS, SMTP, POP3, and IMAP protocols, as well as a load balancer, HTTP cache, and a web server (origin server). deploy' }).getByRole('button', { name: 'deploy' }).click();

  await page.waitForTimeout(1000);

  await page.getByText('Check it out!').click();

  await page.waitForTimeout(1000);

  await page.getByRole('cell', { name: /Deployment Group/ }).locator('i').click();

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').click();

  await page.getByRole('row', { name: 'Name' }).locator('input[type="text"]').fill(random_id + ' nginx test');

  await page.getByRole('row', { name: 'Name' }).getByRole('button').click();

  await page.getByTestId('add-edges-button').click();

  await page.getByPlaceholder('Search...').click();
  await page.getByPlaceholder('Search...').fill('e2e test');

  await page.getByRole('link', { name: 'select row 0 e2e-Test-Do_not_delete NEW endtoend@sixsq.com' }).click();

  await page.getByRole('button', { name: 'Add to deployment group' }).click();

  await page.locator('a:has-text("Save")').click();

  const mainMenu = await page.getByTestId('nuvla-ui-sidebar');

  await mainMenu.getByRole('link', { name: 'deployments'}).click();

  await page.getByRole('link', { name: 'Deployment groups' }).click();

  await page.locator('.ui > .ui > a:nth-child(3)').click();

  await expect(page.getByRole('cell', { name: random_id + ' nginx test' })).toBeVisible();

});


test('delete deployment group', async ({ page }, { config }) => {

 const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  const mainMenu = await page.getByTestId('nuvla-ui-sidebar');

  await mainMenu.getByRole('link', { name: 'deployments'}).click();

  await page.getByRole('link', { name: 'Deployment groups' }).click();
  await expect(page).toHaveURL(`${baseURL}/ui/deployment-groups`);

  await page.getByPlaceholder('Search...').click();

  await page.getByPlaceholder('Search...').fill(random_id + ' nginx');

  await expect(page).toHaveURL(`${baseURL}/ui/deployment-groups?deployment-groups-search=${random_id}%20nginx`);

  await page.getByRole('link', { name: new RegExp(random_id + ' nginx') }).click();

  await page.locator('div[role="listbox"]:has-text("CancelDelete")').click();

  await page.getByTestId('deployment-group-delete-button').click();

  await page.getByText(/There are currently no running deployments/i).click();

  await page.getByRole('button', { name: 'Delete?' }).click();

  await page.getByRole('button', { name: 'Yes: Delete' }).click();

  await expect(page).toHaveURL(`${baseURL}/ui/deployment-groups`);

});


