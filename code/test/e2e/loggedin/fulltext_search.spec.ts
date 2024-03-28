import { test, expect } from '@playwright/test';

test.describe('Full text search', () => {
  for (const pageName of [
    // 'apps', 'deployments',
    'edges',
    // 'data'
  ]) {
    test('on page ' + pageName, async ({ page }, { config }) => {
      const { baseURL } = config.projects[0].use;
      await page.goto(baseURL + '/ui/welcome');
      await page.getByRole('link', { name: pageName }).click();
      const search = page.getByPlaceholder(/Search/);

      await search.pressSequentially('hello world', {delay: 50})
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await search.pressSequentially('test', {delay: 50})

      await expect(page.getByPlaceholder(/Search/)).toHaveValue('hello testworld', { timeout: 100 });

      await page.keyboard.press('Backspace', { delay: 50 });
      await page.keyboard.press('Backspace', { delay: 50 });
      await page.keyboard.press('Backspace', { delay: 50 });
      await page.keyboard.press('Backspace', { delay: 50 });

      await expect(page.getByPlaceholder(/Search/)).toHaveValue('hello world', { timeout: 100 });

      if (pageName === 'apps') {
        await page.getByText('Navigate Projects').click();
        await page.getByText('Marketplace').click();
        await expect(page.getByPlaceholder(/Search/)).toHaveValue('hello world', { timeout: 1000 });
      }
    });
  }
});
