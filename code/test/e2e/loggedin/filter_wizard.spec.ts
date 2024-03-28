import { test, expect } from '@playwright/test';

test.use({ navigationTimeout: 5000, actionTimeout: 5000 });

test('Additional filter wizard', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'edges' }).click();
  await page.getByRole('button', { name: 'Filter' }).click();
  await page.getByText('attribute name').click();
  await page.getByRole('option', { name: 'name', exact: true }).click();
  await page.locator('div[role="alert"]:has-text("operation")').click();
  await page.getByRole('option', { name: 'Equal', exact: true }).getByText('Equal').click();
  await page.getByText('value').click();
  await page.locator('span:has-text("e2e-Test-Do_not_delete")').click();
  await page.getByRole('button', { name: 'Done' }).click();
  await page.waitForURL(`${baseURL}/ui/edges?view=table&nuvlabox=name%3D%27e2e-Test-Do_not_delete%27`);

  await page.waitForResponse('/api/nuvlabox');

  await page.waitForTimeout(300);
  expect(page.locator('tr[role=link]')).toHaveCount(1);

  await page.getByRole('button', { name: 'Filter' }).click();
  await page.getByRole('button', { name: 'remove filter' }).click();
  await page.waitForResponse('/api/nuvlabox');
  await page.waitForTimeout(300);
  expect(await page.locator('tr[role=link]').count()).toBeGreaterThan(1);

  await page.waitForURL(`${baseURL}/ui/edges?view=table`);

  await page.goBack();
  await page.waitForURL(`${baseURL}/ui/edges?view=table&nuvlabox=name%3D%27e2e-Test-Do_not_delete%27`);
  await page.waitForResponse('/api/nuvlabox');
  await page.waitForTimeout(300);
  expect(await page.locator('tr[role=link]').count()).toBe(1);

  await page.goForward();
  await page.waitForURL(`${baseURL}/ui/edges?view=table`);
  await page.waitForResponse('/api/nuvlabox');
  await page.waitForTimeout(300);
  expect(await page.locator('tr[role=link]').count()).toBeGreaterThan(1);

  await page.goBack();
  await page.waitForURL(`${baseURL}/ui/edges?view=table&nuvlabox=name%3D%27e2e-Test-Do_not_delete%27`);
  await page.waitForResponse('/api/nuvlabox');
  await page.waitForTimeout(300);
  expect(await page.locator('tr[role=link]').count()).toBe(1);
  await page.reload();
  await page.waitForURL(`${baseURL}/ui/edges?view=table&nuvlabox=name%3D%27e2e-Test-Do_not_delete%27`);
  await page.waitForResponse('/api/nuvlabox');
  await page.waitForTimeout(300);
  expect(await page.locator('tr[role=link]').count()).toBe(1);
});
