import { test, expect } from '@playwright/test';

test('Language modal not behind main content', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.pause();
  await page.locator('#nuvla-ui-main span:has-text("en")').click();
  await page.getByRole('option', { name: 'English' }).isVisible();
});
