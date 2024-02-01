import { test, expect, Page } from '@playwright/test';

test.use({ actionTimeout: 10000, navigationTimeout: 10000 });

test('CHANGES PROTECTION MODAL TEST 1: opens by main navigation', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await setUp(page, baseURL);

  const url = page.url();

  ////////////////////////////////////
  // TESTING OPEN MODAL BY MAIN NAV //
  const openModal = () => page.getByRole('link', { name: 'deployments' }).click();

  await openModal();

  // ignores changes
  await page.getByRole('button', { name: 'ignore changes' }).click();
  page.waitForURL(new RegExp(`${baseURL}/ui/deployments`), { timeout: 2000 });

  await setUp(page, baseURL);

  // CLICKS X for closing
  await openModal();
  await page.locator('.close').click();
  testSameUrl(page, url, 'Same URL after click on X');

  // NAVIGATE BACK for closing
  await page.goBack();
  testSameUrl(page, url, 'Same URL after navigating back');

  // CLICKS OUTSIDE MODAL for closing
  await page.locator('body > div.ui.page.modals.dimmer.transition.visible.active').click({position: {x: 10, y: 10}});
  expect(page.getByRole('button', { name: 'ignore changes' })).toBeHidden();
  expect(page.url()).toBe(url);

  // Test if all protections are still in place
  // clicks X to close it
  // Open modal
  await openModal();
  await page.locator('.close').click();
  expectModalHidden(page, 'Modal still hides after clicking X 1st time?');
  await page.goBack();
  await page.locator('.close').click();
  expectModalHidden(page, 'Modal still hides after navigating back 2nd time?');
  await openModal();
  await page.getByRole('button', { name: 'ignore changes' }).click();
  expectModalHidden(page, 'Modal still hides after clicking ignore  3rd time?');
  await page.waitForURL(`${baseURL}/ui/deployments`);
});

function testSameUrl(page, url: string, errorMessage = 'Same URL?') {
  expect(page.url(), errorMessage).toBe(url);
}

function expectModalVisible(page, errorMessage = 'Modal visible?') {
  expect(page.getByRole('button', { name: 'ignore changes' }), errorMessage).toBeVisible({ timeout: 2000 });
}

function expectModalHidden(page, errorMessage = 'Modal hidden?', timeout = 2000) {
  expect(page.getByRole('button', { name: 'ignore changes' }), errorMessage).toBeHidden({ timeout });
}

async function setUp(page: Page, baseURL) {
  await page.getByRole('link', { name: 'apps' }).click();
  await page.waitForURL(`${baseURL}/ui/apps`);
  await page.getByRole('link', { name: 'Navigate Projects' }).click();
  await page.waitForURL(`${baseURL}/ui/apps?apps-store-tab=navigate`);
  await page.getByText('DO NOT DELETE --- e2e test project', { exact: true }).click();
  await page.waitForURL(`${baseURL}/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=overview`);
  await page.getByRole('link', { name: 'Details' }).click();
  await page.waitForURL(`${baseURL}/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=details`);
  await page.locator('input[type="input"]').click();
  return page.locator('input[type="input"]').fill('DO NOT DELETE --- e2e test project HELLO');
}
