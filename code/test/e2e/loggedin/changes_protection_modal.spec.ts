import { test, expect } from '@playwright/test';

test('CHANGES PROTECTION MODAL TEST 1a: opens by main navigation, and closes by ignore changes click', async ({
  page,
}, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  //
  await page.pause();

  await setUp(page);
  await page.getByRole('link', { name: 'deployments' }).click();
  await page.getByRole('button', { name: 'ignore changes' }).click();

  expect(page.getByRole('button', { name: 'ignore changes' })).toBeHidden({ timeout: 2000 });
  await page.waitForURL('https://nui.localhost/ui/deployments');
});

test('CHANGES PROTECTION MODAL TEST 1b: opens by main navigation, and closes by navigating back', async ({ page }, {
  config,
}) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  //
  await page.pause();

  await setUp(page);
  const url = page.url();

  await page.getByRole('link', { name: 'deployments' }).click();
  await page.goBack();
  expect(page.getByRole('button', { name: 'ignore changes' })).toBeHidden({ timeout: 2000 });
  expect(page.url()).toBe(url);
});

// TODO:
test.skip('CHANGES PROTECTION MODAL TEST 2: opens by clicking link handled by reitit, and closes by ignore changes click', async ({
  page,
}, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  //
  // await page.pause();

  await page.getByRole('link', { name: 'apps' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps');
  await page.getByRole('link', { name: 'Navigate Apps' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps?apps-store-tab=navigate');
  await page.getByText('DO NOT DELETE --- e2e test project').click();
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=overview');
  await page.getByRole('link', { name: 'Details' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=details');
  await page.locator('input[type="input"]').click();
  await page.locator('input[type="input"]').fill('DO NOT DELETE --- e2e test project. ');
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=details');
  await page.getByRole('button', { name: 'ignore changes' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=details');
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=overview');
});

test.skip('CHANGES PROTECTION MODAL TEST 3: opens by navigating back, 2) and closes by ignore changes click', async ({
  page,
}, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  //
  await page.pause();

  await setUp(page);
  await page.goBack();
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=details');
  await page.getByRole('button', { name: 'ignore changes' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=details');
  await page.waitForURL('https://nui.localhost/ui/apps/do-not-delete--e2e-test-project?apps-project-tab=overview');
});

async function setUp(page) {
  await page.getByRole('link', { name: 'apps' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps');
  await page.getByRole('link', { name: 'Navigate Apps' }).click();
  await page.waitForURL('https://nui.localhost/ui/apps?apps-store-tab=navigate');
  await page.getByText('DO NOT DELETE --- e2e test project').click();
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
