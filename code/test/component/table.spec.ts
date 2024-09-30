import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne, dragAndDrop } from './utils';

async function expectHeadersOrder(table, headers) {
  await expect(table.locator('thead tr th')).toHaveCount(headers.length);
  for (let index = 0 ; index < headers.length ; index++) {
      let header = headers[index];
      await expect(table.locator('thead tr th').nth(index)).toHaveText(header);
  }
}

async function expectColumnData(page, table, colIndex, colData) {
  for (let rowIndex = 0; rowIndex < colData.length ; rowIndex++) {
      let cellData = colData[rowIndex];
      await expect(await table.locator('tbody > tr:nth-child(' + (rowIndex + 1) + ') > td:nth-child(' + colIndex + ')')).toHaveText(cellData);
  };
}

async function openColumnSelectorModal(sceneRoot) {
  await sceneRoot.getByTitle('Columns selector').locator('i').click();
}

test('test column selection, deletion and sorting', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'table-refactor');

  const table = await locatorOne(sceneRoot, 'table.ui');

  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

  expect(table.locator('thead tr')).toHaveCount(1);
  expect(table.locator('tbody tr')).toHaveCount(3);

//   Modal unselect Created column
  openColumnSelectorModal(sceneRoot);
  await sceneRoot.locator('label').filter({ hasText: 'Created' }).click();
  await sceneRoot.getByLabel('update').click();
  await expectHeadersOrder(table, ['Id', 'Size']);

// Modal select defaults columns and cancel
  openColumnSelectorModal(sceneRoot);
  await sceneRoot.getByLabel('Select default columns').click();
  await sceneRoot.getByLabel('cancel').click();
  await expectHeadersOrder(table, ['Id', 'Size']);

// Modal select defaults columns and cancel
  openColumnSelectorModal(sceneRoot);
  await sceneRoot.getByLabel('Select default columns').click();
  await sceneRoot.getByLabel('update').click();
  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

// Expect delete Id column is not possible
  await table.getByRole('button', { name: 'Id' }).hover();
  await expect(table.getByRole('button', { name: 'Id' }).locator('a[aria-label="Delete Column"]')).toHaveCount(0);

// Expect delete Size column is possible
  await table.getByRole('button', { name: 'Size' }).hover();
  await table.getByRole('button', { name: 'Size' }).locator('a[aria-label="Delete Column"]').click();
  await expectHeadersOrder(table, ['Id', 'Created']);

// Sort by Created ascending
  await table.getByRole('button', { name: 'Created' }).click();
  await expectColumnData(page, table, 2, ['1725666894','1725667915','1726074087']);

// Sort by Created descending
  await table.getByRole('button', { name: 'Created' }).click();
  await expectColumnData(page, table, 2, ['1726074087','1725667915','1725666894']);

// Sort by Created descending + Id ascending
  await table.getByRole('button', { name: 'Id' }).click();
  await expectColumnData(page, table, 2, ['1726074087','1725667915','1725666894']);

// No more sorting on Created => Sort by Id ascending
  await table.getByRole('button', { name: 'Created' }).click();
  await expectColumnData(page, table, 2, ['1725666894','1726074087','1725667915']);
});

test('test table draggable columns', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'table-refactor');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

//   Move Created column to first position
  await dragAndDrop(page, table.getByRole('button', { name: 'Created' }), table.getByRole('button', { name: 'Size' }));
  await expectHeadersOrder(table, ['Id', 'Created', 'Size']);
});
