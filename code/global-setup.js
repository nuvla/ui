// global-setup.js
const { chromium, expect } = require('@playwright/test');

export default async (config) => {
  const { baseURL } = config.projects[0].use;

  const { page, browser } = await login(baseURL, config);
  await browser.close();
};

export async function login(baseURL, config) {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  page.setExtraHTTPHeaders({
    'CF-Access-Client-Secret': process.env.CF_CLIENT_SECRET,
    'CF-Access-Client-Id': process.env.CF_CLIENT_ID,
  });
  await page.goto(baseURL + '/ui/sign-in');

  await page.locator('input[name="username"]').click();

  await page.locator('input[name="username"]').fill(process.env.UI_E2E_TEST_USERNAME);

  await page.locator('input[name="username"]').press('Tab');

  await page.locator('input[name="password"]').fill(process.env.UI_E2E_TEST_PASSWORD);

  await page.locator('input[name="password"]').press('Enter');

  await page.waitForURL(/ui\/welcome/);
  // Save signed-in state to 'storageState.json'.
  await page.context().storageState({ path: config.projects[0].use.storageState });
  return { page, browser };
}
