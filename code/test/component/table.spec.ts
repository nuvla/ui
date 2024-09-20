import { test, expect } from '@playwright/test';

test('test', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;

  await page.goto(baseURL + '/?id=sixsq.nuvla.ui.components.table-scenes%2Fsimple-table');

  const iframe = page.frameLocator('iframe.canvas');

  const table = iframe.first().locator('table.ui');
  expect(table).toHaveCount(1);

  expect(table.first().locator('tr')).toHaveCount(2);
});
