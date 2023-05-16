import { test, expect } from '@playwright/test';

test('Changing apps version does not affect other app', async ({ page }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  const marketplaceUrl = baseURL + '/ui/apps';
  await page.goto(baseURL + '/ui/welcome');
  await page.pause();
  await page.getByRole('link', { name: 'Apps' }).click();
  await expect(page).toHaveURL(marketplaceUrl);

  await page.getByPlaceholder('Search...').click();
  await page.getByPlaceholder('Search...').fill('latence');
  await page.waitForURL('https://nui.localhost/ui/apps?apps-store-modules-search=latence');
  await page
    .getByRole('link', {
      name: 'LatenceTech Analyzer Collects, stores and performs real-time analysis on the measurement results (sent by the QoSAgent). Offers real-time observability using ready-to-use dashboards to view results and… Real-time Dashboards Time-Series Database Latency Analytics free trial and then €8.30/day',
    })
    .click();
  await page.waitForURL('https://nui.localhost/ui/apps/latencetech/analyzer/?version=8&apps-tab=overview');
  await page.getByRole('link', { name: 'Versions' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps/latencetech/analyzer/?version=8&apps-tab=versions');
  await page.locator('a:has-text("v7")').click();
  await page.locator('#nuvla-ui-header-breadcrumb').getByText('latencetech').click();
  await page.waitForURL('https://nui.localhost/ui/apps/latencetech?apps-project-tab=overview');
  await page.getByText('LatenceTech QoSAgent').click();
  await page.getByRole('link', { name: 'Versions' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps/latencetech/latencetech?apps-tab=versions');
  await page.getByRole('cell', { name: 'v7' }).isVisible({ timeout: 2000 });
  await page.getByRole('cell', { name: 'v7 <<' }).isHidden({ timeout: 2000 });
});
