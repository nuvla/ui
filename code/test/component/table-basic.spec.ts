import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { expectHeadersOrder, expectHeaderRowCount, expectTableRowCount } from './table';

test('table-basic', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'basic-table');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

  expectHeaderRowCount(table, 1);
  expectTableRowCount(table, 3);
});
