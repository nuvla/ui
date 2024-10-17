import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { selectRow, selectAllClick, expectHeadersOrder, expectSelectedItemsCount } from './table';

test('table-row-selection', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'row-selection');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectHeadersOrder(table, ['', 'Id', 'Size', 'Created']);
  await expectSelectedItemsCount(sceneRoot, 0);

  // select all
  await selectAllClick(table);
  await expect(await table.locator('thead > tr > th:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(1) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(3) > td:nth-child(1) > div > input')).toBeChecked();
  await expectSelectedItemsCount(sceneRoot, 3);

  // unselect all
  await selectAllClick(table);
  await expect(await table.locator('tbody > tr:nth-child(1) > td:nth-child(1) > div > input')).toBeChecked({checked: false});
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked({checked: false});
  await expect(await table.locator('tbody > tr:nth-child(3) > td:nth-child(1) > div > input')).toBeChecked({checked: false});
  await expectSelectedItemsCount(sceneRoot, 0);

  // select first row
  await selectRow(table, 0);
  await expect(await table.locator('tbody > tr:nth-child(1) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked({checked: false});
  await expectSelectedItemsCount(sceneRoot, 1);

  //select second and third rows
  await selectRow(table, 1);
  await selectRow(table, 2);
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(3) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('thead > tr > th:nth-child(1) > div > input')).toBeChecked();
  await expectSelectedItemsCount(sceneRoot, 3);
});

