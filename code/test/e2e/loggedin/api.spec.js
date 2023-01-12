import { test, expect } from '@playwright/test';

test('Navigation api pages and title check', async ({ page }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  const apiUrl = baseURL + '/ui/api';

  await page.getByRole('link', { name: 'Api' }).click();
  await expect(page).toHaveURL(apiUrl);

  let collections = ['user', 'session']
    for (const collection of collections) {
      const urlToVisit = apiUrl + '/' + collection;
      await page.getByRole('combobox', { name: 'resource type' }).locator('i').click();
      await page.getByRole('option', { name: collection }).click();
      await expect(page).toHaveURL(urlToVisit);
      await expect(page).toHaveTitle('Nuvla api/' + collection)
    }

});
