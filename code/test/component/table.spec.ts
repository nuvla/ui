import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';

test('test', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'table-refactor');

  const table = await sceneRoot.locator('table.ui');
  expect(table).toHaveCount(1);
  expect(table.first().locator('thead tr th').first()).toHaveText('Id');
  expect(table.first().locator('thead tr th').nth(1)).toHaveText('Size');
  expect(table.first().locator('thead tr th').nth(2)).toHaveText('Created');

  expect(table.first().locator('thead tr')).toHaveCount(1);
  expect(table.first().locator('tbody tr')).toHaveCount(3);
  expect(table.first().getByRole('cell', { name: 'Id' })).toBeVisible();

  await sceneRoot.getByTitle('Columns selector').locator('i').click();
  await sceneRoot.locator('label').filter({ hasText: 'Created' }).click();
  await sceneRoot.getByLabel('update').click();
  expect(table.first().locator('thead tr th')).toHaveCount(2);
  
  expect(table.first().locator('thead tr th').first()).toHaveText('Id');
  expect(table.first().locator('thead tr th').nth(1)).toHaveText('Size');

  // await page.frameLocator('iframe[title="Component scene"]').frameLocator('iframe').getByLabel('update').click();
  // table.first().locator('thead tr td').count();
//     await expect(page.frameLocator('iframe[title="Component scene"]').frameLocator('iframe').getByRole('cell', { name: 'Id' })).toBeVisible();
//     await expect(page.frameLocator('iframe[title="Component scene"]').frameLocator('iframe').getByRole('cell', { name: 'Size' })).toBeVisible();
//     await expect(page.frameLocator('iframe[title="Component scene"]').frameLocator('iframe').getByRole('cell', { name: 'Created' })).toBeVisible();

});

