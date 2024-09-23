import { test, expect } from '@playwright/test';

test('test', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;

  await page.goto(baseURL + '/?id=sixsq.nuvla.ui.components.table-scenes%2Fsimple-table');

  const iframe = await page.frameLocator('iframe.canvas').first();

  const table = await iframe.locator('table.ui');

  console.log("table content:" + await table.innerHTML());

  expect(table).toHaveCount(1);

  expect(table.first().locator('tr')).toHaveCount(2);
});

