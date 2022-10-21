import { test, expect } from '@playwright/test';
import { mockedInfrasctureServiceData, credentials } from './mockData';

test('Testing app deployment', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'apps' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/apps');
  await page.getByText('All Apps').click();
  await page.getByPlaceholder('Search...').click();
  await page.getByPlaceholder('Search...').fill('Rstu');
  await page.getByRole('link', { name: 'RStudio RStudio deployment with generated password launch' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/apps/examples/rstudio');

  // Start mocking all network requests from recording...
  await page.routeFromHAR('./tests/loggedin/network.har');

  // ...but override these requests
  await page.route('api/infrastructure-service', (route) => {
    let payload = route.request().postDataJSON();
    route.fulfill({
      status: 200,
      body: JSON.stringify(mockedInfrasctureServiceData[payload.filter]),
    });
  });
  await page.route('api/credential', (route) => {
    let payload = route.request().postDataJSON();
    const responseBody = payload.filter ? credentials['PUT'][payload.filter] : credentials['GET'];

    route.fulfill({
      status: 200,
      body: JSON.stringify(responseBody),
    });
  });
  await page.getByText('Launch').click();

  await page.getByText('Swarm Edging').nth(1).click();

  await page.getByRole('button', { name: 'force launch' }).nth(1).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/deployment/13662720-83db-467b-9d25-e01da99e3c0b');
});
