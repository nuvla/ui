import { test, expect } from '@playwright/test';

test('Marketplace shows only published apps', async ({ page }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  const marketplaceUrl = baseURL + '/ui/apps';
  await page.goto(baseURL + '/ui/welcome');
  await page.route('/api/module', async (route) => {
    const request = route.request();
    const payload = request.postDataJSON();

    expect(payload.filter.includes('published=true')).toBeTruthy();
    return route.continue();
  });
  await page.getByRole('link', { name: 'Apps' }).click();
  await expect(page).toHaveURL(marketplaceUrl);

  await page.locator('a.ui.card').nth(0).waitFor();
  let elements = await page.$$('a.ui.card');

  let hrefs = await Promise.all(elements.map(async (elem) => elem.getAttribute('href')));
  expect(hrefs.length).toBeGreaterThan(0);
  await page.route('/api/module', async (route) => {
    return route.continue();
  });

  await elements[0].click();
  const appdetails = await page.locator('.nuvla-apps');
  await appdetails.getByRole('link', { name: 'Deployments' }).click();
  await page.waitForURL(/apps-tab=deployments$/);
  await page.locator('#nuvla-ui-header-breadcrumb').getByText('Apps').click();
  await page.waitForURL(`${baseURL}/ui/apps`);
  await page.locator('a.ui.card').nth(1).click();
  await page.waitForURL(/apps-tab=deployments$/);
  expect(page.locator('.nuvla-apps').getByRole('link', { name: 'Deployments' })).toHaveClass(/active/);
  await page.locator('#nuvla-ui-header-breadcrumb').getByText('Apps').click();
  await page.waitForURL(`${baseURL}/ui/apps`);
  await page.getByRole('link', { name: 'Navigate Apps' }).click();
  await page.waitForURL(`${baseURL}/ui/apps?apps-store-tab=navigate`);
});
