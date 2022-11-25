import { test, expect } from '@playwright/test';

test.describe('Full text search', () => {
  for (const pageName of ['apps', 'deployments', 'edges', 'data']) {
    test.skip('on page ' + pageName, async ({ page, context }, { project, config }) => {
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
      await page.keyboard.press('t');
      await page.keyboard.press('e');
      await page.keyboard.press('s');
      await page.keyboard.press('t');

      await expect(page.getByPlaceholder('Search')).toHaveValue('hello testworld', { timeout: 100 });

      await page.keyboard.press('Backspace');
      await page.keyboard.press('Backspace');
      await page.keyboard.press('Backspace');
      await page.keyboard.press('Backspace');

      await expect(page.getByPlaceholder('Search')).toHaveValue('hello world', { timeout: 100 });

      if (pageName === 'apps') {
        await page.getByText('Navigate Apps').click();
        await page.getByText('Marketplace').click();
        await expect(page.getByPlaceholder('Search')).toHaveValue('hello world', { timeout: 1000 });
      }
    });
  }
});
