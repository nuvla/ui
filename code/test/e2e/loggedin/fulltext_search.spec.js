import { test, expect } from '@playwright/test';

test.describe('Full text search', () => {
  for (const pageName of ['apps', 'deployments', 'edges', 'data']) {
    test('on page ' + pageName, async ({ page, context }, { project, config }) => {
      const { baseURL } = config.projects[0].use;
      await page.goto(baseURL + '/ui/welcome');
      await page.getByRole('link', { name: pageName }).click();
      // await expect(page).toHaveURL('https://nui.localhost/ui/apps');
      await page.getByPlaceholder('Search').click();

      for (const char of 'hello world') {
        await page.keyboard.press(char);
      }
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.keyboard.press('ArrowLeft');
      await page.getByPlaceholder('Search').press('t');
      await page.getByPlaceholder('Search').press('e');
      await page.getByPlaceholder('Search').press('s');
      await page.getByPlaceholder('Search').press('t');

      await expect(page.getByPlaceholder('Search')).toHaveValue('hello testworld', { timeout: 100 });
    });
  }
});
