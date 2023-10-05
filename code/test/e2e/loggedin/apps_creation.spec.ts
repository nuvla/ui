import { test, expect } from '@playwright/test';

test.use({ navigationTimeout: 5000, actionTimeout: 5000 });

test('Creating a new app', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'Apps' }).nth(0).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/apps`));

  await page.getByText('Navigate Projects').nth(0).click();
  await expect(page).toHaveURL(`${baseURL}/ui/apps?apps-store-tab=navigate`);

  await page.getByText('DO NOT DELETE --- e2e test project').click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/apps/do-not-delete--e2e-test-project`));

  await page.locator('a:has-text("Add")').click();
  await page.getByRole('link', { name: 'Docker Application' }).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/New%20Application?subtype=application&apps-tab=details`
  );

  const NEW_APP_NAME = `e2e-test-apps-creation-${new Date().toISOString()}`;
  const NEW_APP_NAME_SANITZIED = NEW_APP_NAME.replace(/-|:|\./g, '').toLowerCase();

  await page.locator('input[type="input"]').click();
  await page.locator('input[type="input"]').fill(NEW_APP_NAME);
  await page.locator('a:has-text("Save")').click();
  await page.getByText('Validation error!The form is invalid. Please review the fields in red.').click();
  await page.locator('.cm-activeLine').first().click();
  await page.locator('.cm-activeLine').first().fill('Hi there');
  await page.getByRole('link', { name: 'Docker' }).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/New%20Application?subtype=application&apps-tab=docker`
  );
  await page.getByText('Please describe your Docker stack in this docker compose field').click();
  await page.locator('role=textbox').click();
  await page.getByRole('textbox').fill('How are you');
  await page.locator('a:has-text("Save")').click();
  await page.getByPlaceholder('Commit message - explicit is better').click();
  await page.getByPlaceholder('Commit message - explicit is better').fill('first commit');
  await page.getByRole('button', { name: 'save' }).click();

  await page.waitForURL(`${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=docker`);
  await page.getByRole('link', { name: 'Overview' }).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=overview`
  );
  await page.getByRole('link', { name: 'Details' }).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=details`
  );
  await page.getByRole('link', { name: 'Deployments' }).nth(1).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=deployments`
  );
  await page.getByRole('link', { name: 'EULA' }).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=license`
  );
  await page.getByRole('link', { name: 'Docker' }).click();
  await page.waitForURL(`${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=docker`);
  await page.getByRole('link', { name: 'Configuration' }).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=configuration`
  );
  await page.getByRole('link', { name: 'Versions' }).click();
  await page.waitForURL(
    `${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=versions`
  );
  await page.getByRole('link', { name: 'Share' }).click();
  await page.waitForURL(`${baseURL}/ui/apps/do-not-delete--e2e-test-project/${NEW_APP_NAME_SANITZIED}?apps-tab=share`);

  await page.locator('a:has-text("Delete")').nth(1).click();
  await page.getByText('I understand that deleting this application is permanent and cannot be undone.').click();
  await page.getByRole('button', { name: 'delete' }).click();
  await page.waitForURL(new RegExp(`${baseURL}/ui/apps`));

  await page.getByRole('link', { name: 'My Apps' }).click();
  await page.waitForURL(`${baseURL}/ui/apps?apps-store-tab=myapps`);
  await page.getByPlaceholder('Search...').click();
  await page.getByPlaceholder('Search...').fill('e2e test apps creation');
  await page.waitForURL(`${baseURL}/ui/apps?apps-store-tab=myapps&apps-store-modules-search=e2e+test+apps+creation`);
  await page.getByPlaceholder('Search...').click();
  await page.getByPlaceholder('Search...').fill('creation');

  await page.waitForResponse('api/module');
  expect(await page.getByRole('link', { name: new RegExp('-creation') }).count()).toBe(0);
});
