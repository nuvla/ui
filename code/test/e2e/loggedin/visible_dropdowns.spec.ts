import { test, expect } from '@playwright/test';

test('Language modal not behind main content', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.locator('#nuvla-ui-main span:has-text("en")').click();
  await page.getByRole('option', { name: 'English' }).isVisible();
  await page.getByRole('option', { name: 'Français' }).isVisible();

  const url = `${baseURL}/ui/apps?apps-store-tab=myapps`;
  await page.getByRole('link', { name: 'apps' }).click();
  await page.getByRole('link', { name: 'My Apps' }).click();
  await page.waitForURL(url);
  await page
        .getByRole('button', { name: /(free trial)|(deploy)/ })
        .nth(0)
        .click();
  await page.locator('a:has-text("Application version")').click();
  await page
    .getByText(/^v\d+\s\|/)
    .nth(0)
    .click();
  await page.getByRole('option').nth(1).isVisible();
  await page.locator('.close').click();
});
