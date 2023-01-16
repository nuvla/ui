import { test, expect } from '@playwright/test';

test('Marketplace shows only published apps', async ({ page }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  const marketplaceUrl = baseURL + '/ui/apps';
  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'Apps' }).click();
  await expect(page).toHaveURL(marketplaceUrl);

  const appCards = await page.locator('a.ui.card').nth(0).waitFor();
  const elements = await page.$$('a.ui.card');

  let hrefs = await Promise.all(elements.map(async (elem) => elem.getAttribute('href')));
  expect(hrefs.length).toBeGreaterThan(0);
  for (const href of hrefs.slice(0, 4)) {
    const appUrlToVisit = baseURL + href;
    await page.goto(appUrlToVisit);
    await page.locator('a:has-text("Versions")').click();
    console.log('Testing:', appUrlToVisit);
    expect(page.locator('tr.active > td:nth-child(2) > i')).toHaveClass('teal check icon');
  }
});
