import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { openColumnSelectorModal, expectTableRowCount } from './table';

test('table-global-filter', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'global-filter');

  const table = await locatorOne(sceneRoot, 'table.ui');
  const filterInput = await locatorOne(sceneRoot, '.global-filter > input');

  await expectTableRowCount(table, 3);

  // global filter: 2 rows contain 1725, in column Created
  await filterInput.fill('1725');
  await expectTableRowCount(table, 2);

  // Disable global filtering => Global filter is not applied
  await sceneRoot.getByTestId('checkbox-enable-global-filter').click();
  await expectTableRowCount(table, 3);

  // Enable back global filtering
  await sceneRoot.getByTestId('checkbox-enable-global-filter').click();
  await expectTableRowCount(table, 2);

  // Modal unselect Created column
  openColumnSelectorModal(sceneRoot);
  await sceneRoot.locator('label').filter({ hasText: 'Created' }).click();
  await sceneRoot.getByLabel('update').click();

  // Filtering does not apply to invisible columns
  await expectTableRowCount(table, 0);

  // Clear the filter
  await filterInput.fill('');
  await expectTableRowCount(table, 3);
});
