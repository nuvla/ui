import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    openColumnSelectorModal,
    getColumnHeader,
    getColumnHeaderLabel,
    getDeleteColumnButton,
    expectHeadersOrder
} from './table';

test('table-column-customization', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'column-customization');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

  // Modal unselect Created column
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
  await getColumnHeaderLabel(table, 'Id').hover();
  await expect(getDeleteColumnButton(table, 'Id')).toHaveCount(0);

  // Expect delete Size column is possible
  await getColumnHeader(table, 'Size').hover();
  await getDeleteColumnButton(table, 'Size').click();
  await expectHeadersOrder(table, ['Id', 'Created']);
});
