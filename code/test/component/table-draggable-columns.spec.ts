import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne, dragAndDrop } from './utils';
import { getColumnHeader, expectHeadersOrder } from './table';

test('table-draggable-columns', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'column-customization');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

  // Move Created column to Size position
  await dragAndDrop(page, getColumnHeader(table, 'Created'), getColumnHeader(table, 'Size'));
  await expectHeadersOrder(table, ['Id', 'Created', 'Size']);

  // Move Created column to original position
  await dragAndDrop(page, getColumnHeader(table, 'Size'), getColumnHeader(table, 'Created'));
  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

  // Move Created column to first position
  await dragAndDrop(page, getColumnHeader(table, 'Created'), getColumnHeader(table, 'Id'));
  await expectHeadersOrder(table, ['Created', 'Size', 'Id']);
});
