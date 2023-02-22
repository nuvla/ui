import { test, expect, Page } from '@playwright/test';

test.use({ actionTimeout: 10000, navigationTimeout: 10000 });

test('CHANGES PROTECTION MODAL TEST 1: opens by main navigation', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  //
  // await page.pause();

  await setUp(page);

  // await page.pause();
  const url = page.url();

  ////////////////////////////////////
  // TESTING OPEN MODAL BY MAIN NAV //
  const openModal = () => page.getByRole('link', { name: 'deployments' }).click();

  await openModal();

  // ignores changes
  await page.getByRole('button', { name: 'ignore changes' }).click();
  expectModalHidden(page, 'Modal hidden after ignoring changes');
  page.waitForURL(new RegExp('https://nui.localhost/ui/deployments'), { timeout: 2000 });

  await setUp(page);

  // await page.pause();

  // CLICKS X for closing
  await openModal();
  await page.waitForTimeout(200);
  await page.locator('.close').click();
  expectModalHidden(page, 'Modal hidden after click on X', 200);
  testSameUrl(page, url, 'Same URL after click on X');

  // NAVIGATE BACK for closing
  await openModal();
  await page.waitForTimeout(200);
  await page.goBack();
  expectModalHidden(page, 'Modal hidden after navigating back');
  testSameUrl(page, url, 'Same URL after navigating back');

  // CLICKS OUTSIDE MODAL for closing
  await openModal();
  // next two lines do the same, only use need one
  await page.waitForTimeout(100);
  await page.evaluate(() => document.querySelector('[data-testid=protection-modal]')?.parentElement?.click());
  // await page.getByTestId('protection-modal').locator('..').click();
  expect(page.getByRole('button', { name: 'ignore changes' })).toBeHidden();
  expect(page.url()).toBe(url);

  // Test if all protections are still in place
  // clicks X to close it
  // Open modal
  await openModal();
  await page.locator('.close').click({ timeout: 2000 });
  expectModalHidden(page, 'Modal still hides after clicking X?');
  await page.goBack();
  await page.locator('.close').click({ timeout: 2000 });
});

test.skip('CHANGES PROTECTION MODAL TEST 1b: opens by main navigation, and closes by navigating back', async ({
  page,
}, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  //
  // await page.pause();

  await setUp(page);
  const url = page.url();

  await page.getByRole('link', { name: 'deployments' }).click();
  await page.goBack();
  expect(page.getByRole('button', { name: 'ignore changes' })).toBeHidden({ timeout: 2000 });
  expect(page.url()).toBe(url);
});

function testSameUrl(page, url: string, errorMessage = 'Same URL?') {
  expect(page.url(), errorMessage).toBe(url);
}

function expectModalVisible(page, errorMessage = 'Modal visible?') {
  expect(page.getByRole('button', { name: 'ignore changes' }), errorMessage).toBeVisible({ timeout: 2000 });
}

function expectModalHidden(page, errorMessage = 'Modal hidden?', timeout = 0) {
  expect(page.getByRole('button', { name: 'ignore changes' }), errorMessage).toBeHidden({ timeout });
}

async function setUp(page: Page) {
  await page.getByRole('link', { name: 'apps' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps');
  await page.getByRole('link', { name: 'Navigate Apps' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps?apps-store-tab=navigate');
  // await page.pause();
  await page.getByText('DO NOT DELETE --- e2e test project', { exact: true }).click();
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=overview');
  await page
    .locator('main:has-text("Validation error!The form is invalid. Please review the fields in red.Oops can\'t")')
    .getByRole('button')
    .click();
  await page.waitForURL(
    'https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=overview&apps-tab=details'
  );
  await page.getByRole('link', { name: 'Details' }).click();
  await page.waitForURL(
    'https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=details&apps-tab=details'
  );
  await page.locator('input[type="input"]').click();
  return page.locator('input[type="input"]').fill('DO NOT DELETE --- e2e test project HELLO');
}
