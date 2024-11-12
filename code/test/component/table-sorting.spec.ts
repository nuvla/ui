import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { getColumnHeaderLabel, expectHeadersOrder, expectTableRowCount, expectColumnData } from './table';

test('table-sorting', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'sorting');

  const table = await locatorOne(sceneRoot, 'table.ui');

  await expectHeadersOrder(table, ['Id', 'Size', 'Created', 'Not sortable']);

  await expect(table.locator('thead tr')).toHaveCount(1);
  await expectTableRowCount(table, 3);

  // Sorted by default by Created ascending by scene definition
  await expectColumnData(page, table, 'Created', ['1725666894','1725667915','1726074087']);

  // Sort by Created descending
  await getColumnHeaderLabel(table, 'Created').click();
  await expectColumnData(page, table, 'Created', ['1726074087','1725667915','1725666894']);

  // Sort by Created descending + Id ascending
  await getColumnHeaderLabel(table, 'Id').click();
  await expectColumnData(page, table, 'Created', ['1726074087','1725667915','1725666894']);

  // No more sorting on Created => Sort by Id ascending
  await getColumnHeaderLabel(table, 'Created').click();
  await expectColumnData(page, table, 'Created', ['1725666894','1726074087','1725667915']);

  // Disable sorting => data shown in the order in which it is passed in
  await sceneRoot.getByTestId('checkbox-enable-sorting').click();
  await expectColumnData(page, table, 'Created', ['1726074087','1725667915','1725666894']);

  // Try to sort by Id descending, but expect no effect as sorting is not enabled
  await getColumnHeaderLabel(table, 'Id').click();
  await expectColumnData(page, table, 'Created', ['1726074087','1725667915','1725666894']);

  // Enable sorting again and then sort by Id descending
  await sceneRoot.getByTestId('checkbox-enable-sorting').click();
  await expectColumnData(page, table, 'Created', ['1725666894','1726074087','1725667915']);
  await getColumnHeaderLabel(table, 'Id').click();
  await expectColumnData(page, table, 'Created', ['1725667915','1726074087','1725666894']);
});
