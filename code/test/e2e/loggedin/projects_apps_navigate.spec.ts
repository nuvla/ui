import { test, expect } from '@playwright/test';

test('Navigate from projects to apps', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'Apps' }).nth(0).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/apps`));

  await page.getByText('Navigate Apps').nth(0).click();
  await expect(page).toHaveURL(`${baseURL}/ui/apps?apps-store-tab=navigate`);

  await page.getByText('DO NOT DELETE --- e2e test project').click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/apps/do-not-delete--e2e-test-project`));

  await page.getByText('DO NOT DELETE -- e2e test app').click();
  await expect(page).toHaveURL(`${baseURL}/ui/apps/do-not-delete--e2e-test-project/do-not-delete--e2e-test-app?apps-tab=overview`);
  await expect(page).toHaveTitle('Nuvla apps/do-not-delete--e2e-test-project/do-not-delete--e2e-test-app');
  await expect(page.getByRole('cell', { name: 'DO NOT DELETE -- e2e test app'})).toBeVisible();

});
