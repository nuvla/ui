import { test, expect } from '@playwright/test';

async function delay(ms = 1000) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

test.describe('Full text search', () => {
  for (const pageName of [
    // 'apps', 'deployments',
    'edges',
    'data',
  ]) {
    test('on page ' + pageName, async ({ page }, { config }) => {
      const { baseURL } = config.projects[0].use;
      await page.goto(baseURL + '/ui/welcome');
      await page.getByRole('link', { name: pageName }).click();
      const search = page.getByPlaceholder(/Search/);

      for (const char of 'hello world') {
        await search.type(char, { delay: 50 });
      }
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await page.keyboard.press('ArrowLeft', { delay: 50 });
      await search.type('t', { delay: 50 });
      await search.type('e', { delay: 50 });
      await search.type('s', { delay: 50 });
      await search.type('t', { delay: 50 });

      await expect(page.getByPlaceholder(/Search/)).toHaveValue('hello testworld', { timeout: 100 });

      await page.keyboard.press('Backspace', { delay: 50 });
      await page.keyboard.press('Backspace', { delay: 50 });
      await page.keyboard.press('Backspace', { delay: 50 });
      await page.keyboard.press('Backspace', { delay: 50 });

      await expect(page.getByPlaceholder(/Search/)).toHaveValue('hello world', { timeout: 100 });

      if (pageName === 'apps') {
        await page.getByText('Navigate Apps').click();
        await page.getByText('Marketplace').click();
        await expect(page.getByPlaceholder(/Search/)).toHaveValue('hello world', { timeout: 1000 });
      }
    });
  }
});
