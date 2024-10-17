import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { selectPageSize, expectPaginationState, expectTableRowCount } from './table';

test('table-pagination', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'pagination');

  const table = await locatorOne(sceneRoot, 'table.ui');
  const paginationDiv = await locatorOne(sceneRoot, 'div.uix-pagination');
  const paginationNavigation = await locatorOne(paginationDiv, '>div.uix-pagination-navigation');
  const firstItemLink = await locatorOne(paginationNavigation, 'a[type="firstItem"]');
  const prevItemLink = await locatorOne(paginationNavigation, 'a[type="prevItem"]');
  const nextItemLink = await locatorOne(paginationNavigation, 'a[type="nextItem"]');
  const lastItemLink = await locatorOne(paginationNavigation, 'a[type="lastItem"]');

  // Move through the pages and check the pagination state
  await expectPaginationState(page, table, paginationDiv, 303, 25, 1);
  await nextItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 303, 25, 2);
  await prevItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 303, 25, 1);
  await lastItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 303, 25, 13);
  await firstItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 303, 25, 1);

  // Change the page size and repeat the checks
  await selectPageSize(paginationDiv, 50);
  await expectPaginationState(page, table, paginationDiv, 303, 50, 1);
  await lastItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 303, 50, 7);

  // Disable pagination => all passed in data is shown in the table and pagination control is not shown
  await sceneRoot.getByTestId('checkbox-enable-pagination').click();
  await expectTableRowCount(table, 303);
  await expect(sceneRoot.locator('div.uix-pagination')).toHaveCount(0);
});
