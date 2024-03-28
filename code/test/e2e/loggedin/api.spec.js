import { test, expect } from '@playwright/test';

test('Navigation api pages and title check', async ({ page }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  const apiUrl = baseURL + '/ui/api';

  await page.getByRole('link', { name: 'Api' }).click();
  await expect(page).toHaveURL(apiUrl);
  let collections = ['user', 'session', 'user-template'];
  for (const collection of collections) {
    const urlToVisit = apiUrl + '/' + collection;
    await page.getByLabel('resource type').getByRole('textbox').click();
    await page.getByRole('option', { name: collection , exact: true}).click();
    await expect(page).toHaveURL(urlToVisit);
    await expect(page).toHaveTitle('Nuvla api/' + collection);
  }
  await page.waitForTimeout(300);
  expect(await page.getByRole('row').count()).toBeGreaterThan(1);
  await page.getByRole('cell', { name: 'id' }).locator('a').nth(1).click();
  await expect(page.getByPlaceholder('e.g. created:desc, ...')).toHaveValue('id:asc');
  await page.getByRole('cell', { name: 'id' }).locator('a').nth(1).click();
  await expect(page.getByPlaceholder('e.g. created:desc, ...')).toHaveValue('id:desc');
  await page.getByRole('cell', { name: 'id' }).locator('a').nth(1).click();
  console.info(await page.getByPlaceholder('e.g. created:desc, ...').inputValue());
  await expect(page.getByPlaceholder('e.g. created:desc, ...')).toHaveValue('');
  await page.getByRole('cell', { name: 'name' }).locator('a').nth(1).click();
  await expect(page.getByPlaceholder('e.g. created:desc, ...')).toHaveValue('name:asc');
  await expect(page).toHaveURL(
    apiUrl + '/user-template?first=0&last=20&filter=&orderby=name%3Aasc&select=&aggregation='
  );
  await page.getByRole('cell', { name: 'id' }).locator('a').nth(1).click();
  await expect(page.getByPlaceholder('e.g. created:desc, ...')).toHaveValue('id:asc');
  await expect(page).toHaveURL(apiUrl + '/user-template?first=0&last=20&filter=&orderby=id%3Aasc&select=&aggregation=');
});
