import { test, expect } from '@playwright/test';

test.use({ navigationTimeout: 5000, actionTimeout: 5000 });

test('Creating a new project', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'Apps' }).nth(0).click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/apps`));

  await page.getByText('Navigate Apps').nth(0).click();
  await expect(page).toHaveURL(`${baseURL}/ui/apps?apps-store-tab=navigate`);
  await page.locator('a:has-text("Add")').click();
  await page.getByRole('link', { name: 'Project' }).nth(1).click();
  await expect(page).toHaveURL(`${baseURL}/ui/apps/New%20Project?subtype=project&apps-project-tab=details`, {
    timeout: 2000,
  });
  await page.locator('input[type="input"]').click();
  const newProjectName = `ThisIsJustATest${Math.round(Math.random() * 1000)}`;
  await page.locator('input[type="input"]').fill(newProjectName);
  await page
    .getByText('# Project Description PlaceholderThis is a generic placeholder that you should r')
    .fill('TESTING');
  await page.pause();
  await page.locator('a:has-text("Save")').click();
  await page.getByRole('button', { name: 'save' }).click();
  await expect(page).toHaveURL(`${baseURL}/ui/apps/${newProjectName.toLowerCase()}?apps-project-tab=overview`);
  await page.locator('a:has-text("Delete")').click();
  await page.getByText('I understand that deleting this application is permanent and cannot be undone.').click();
  await page.getByRole('button', { name: 'Delete Application' }).click();
  await page.getByRole('button', { name: 'Yes: Delete Application' }).click();
  await expect(page).toHaveURL(`${baseURL}/ui/apps?apps-store-tab=appstore`);
});
