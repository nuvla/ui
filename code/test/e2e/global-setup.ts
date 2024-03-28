import { test } from '@playwright/test';


const authFile = 'storageState.json';

test('authenticate', async ({page}) => {

  await  page.setExtraHTTPHeaders({
    'CF-Access-Client-Secret': process.env.CF_CLIENT_SECRET!,
    'CF-Access-Client-Id': process.env.CF_CLIENT_ID!,
  });

  await page.goto(process.env.UI_BASE_URL!);
  // hide re-frame-10x or local tests fail
  await page.evaluate(`window.localStorage.setItem('day8.re-frame-10x.show-panel', '"false"')`);
  // set feature flags if needed
  // await page.evaluate(`window.localStorage.setItem('nuvla.ui.feature-flags', JSON.stringify(["deployment-set","applications-sets"]))`);

  await page.goto(process.env.UI_BASE_URL!);
  await page.getByText('login').click();

  await page.locator('input[name="username"]').click();

  await page.locator('input[name="username"]').fill(process.env.UI_E2E_TEST_USERNAME!);

  await page.locator('input[name="username"]').press('Tab');

  await page.locator('input[name="password"]').fill(process.env.UI_E2E_TEST_PASSWORD!);

  await page.locator('input[name="password"]').press('Enter');

  await page.waitForURL(/ui\/welcome/);
  // Save signed-in state to 'storageState.json'.
  await page.context().storageState( { path: authFile});
  });
