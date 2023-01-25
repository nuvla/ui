import { test, expect } from '@playwright/test';

test('Language modal not behind main content', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.locator('#nuvla-ui-main span:has-text("en")').click();
  await page.getByRole('option', { name: 'English' }).isVisible();

  const url = `${baseURL}/ui/apps`;

  await page.getByRole('link', { name: 'apps' }).click();
  await page.waitForURL(baseURL);

  for (let i of [0, 1, 2, 3, 4, 5, 6, 7]) {
    await page.pause();
    console.log('testing app no', i);
    await page
      .getByText(/(free trial)|(launch)/)
      .nth(i)
      .click();
    await page.locator('a:has-text("Application version")').click();
    await page
      .getByText(/^v\d+\s\|/)
      .nth(0)
      .click();
    await page.getByRole('option').nth(1).isVisible();
    await page.locator('.close').click();
    await page.getByRole('link', { name: 'apps' }).click();
    await page.waitForURL(baseURL);
  }
});
