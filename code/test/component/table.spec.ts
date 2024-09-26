import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';

function expectHeadersOrder(table, headers) {
  expect(table.first().locator('thead tr th')).toHaveCount(headers.length);
  headers.map((header, index) => {
      expect(table.first().locator('thead tr th').nth(index)).toHaveText(header);
  });
}

async function openColumnSelectorModal(sceneRoot) {
  await sceneRoot.getByTitle('Columns selector').locator('i').click();
}



test('test', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'table-refactor');

  const table = await sceneRoot.locator('table.ui');
  expect(table).toHaveCount(1);

  expectHeadersOrder(table, ['Id', 'Size', 'Created']);

  expect(table.first().locator('thead tr')).toHaveCount(1);
  expect(table.first().locator('tbody tr')).toHaveCount(3);

//   Modal unselect Created column
  openColumnSelectorModal(sceneRoot);
  await sceneRoot.locator('label').filter({ hasText: 'Created' }).click();
  await sceneRoot.getByLabel('update').click();
  expectHeadersOrder(table, ['Id', 'Size']);

// Modal select defaults columns and cancel
  openColumnSelectorModal(sceneRoot);
  await sceneRoot.getByLabel('Select default columns').click();
  await sceneRoot.getByLabel('cancel').click();
  expectHeadersOrder(table, ['Id', 'Size']);

// Modal select defaults columns and cancel
  openColumnSelectorModal(sceneRoot);
  await sceneRoot.getByLabel('Select default columns').click();
  await sceneRoot.getByLabel('update').click();
  expectHeadersOrder(table, ['Id', 'Size', 'Created']);

// Expect delete Id column is not possible
  await table.first().getByRole('cell', { name: 'Id' }).hover();
  await expect(table.first().getByRole('cell', { name: 'Id' }).locator('a[aria-label="Delete Column"]')).toHaveCount(0);

// Expect delete Size column is possible
  await table.first().getByRole('cell', { name: 'Size' }).hover();
  await table.first().getByRole('cell', { name: 'Size' }).locator('a[aria-label="Delete Column"]').click();
  expectHeadersOrder(table, ['Id', 'Created']);


});

