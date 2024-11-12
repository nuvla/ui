import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { clickRow, expectClickedRowsCount } from './table';

test('table-clickable-rows', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'clickable-rows');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectClickedRowsCount(sceneRoot, 0);

  await clickRow(table, 0);
  await expectClickedRowsCount(sceneRoot, 1);

  await clickRow(table, 1);
  await expectClickedRowsCount(sceneRoot, 2);

  await clickRow(table, 2);
  await expectClickedRowsCount(sceneRoot, 3);
});
