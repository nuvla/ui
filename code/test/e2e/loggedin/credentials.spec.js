import { test, expect } from '@playwright/test';

test.skip('Creates and deletes api key credentials', async ({ page }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  const credentialsUrl = baseURL + '/ui/credentials';
  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'Credentials' }).click();
  await expect(page).toHaveURL(credentialsUrl);

  await page.getByText('Add').click();
  await page.locator('a:has-text("Api-Key")').click();
  await page.getByRole('row', { name: 'name' }).locator('input[type="input"]').click();
  await page.getByRole('row', { name: 'name' }).locator('input[type="input"]').fill('e2e-testing');
  await page.getByRole('row', { name: 'name' }).locator('input[type="input"]').press('Tab');
  await page.getByRole('row', { name: 'description' }).locator('input[type="input"]').fill('key');

  await page.getByRole('button', { name: 'create' }).click();
  await page.getByText('Secret: ');
  await page.locator('div:nth-child(3) > .ui > div > .right > .clone').click();

  // TODO: should we test this?
  await page.locator('.close').click();

  await page.pause();
  await page.getByRole('row', { name: 'e2e-testing key api-key' }).locator('i').first().click();
  await page.getByText('I understand that deleting this credential is permanent and cannot be undone. Th').click();
  await page.getByRole('button', { name: 'delete' }).click();
});
