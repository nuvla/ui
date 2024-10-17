import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    selectAllClick,
    selectPageSize,
    getColumnHeaderLabel,
    expectTableRowCount,
    expectSelectedItemsCount,
    expectPaginationState
} from './table';

test('table-filtering-sorting-pagination-row-selection', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'filter-sort-paginate-select');
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

  // Select all rows
  await selectAllClick(table);
  await expectSelectedItemsCount(sceneRoot, 303);

  // Apply a global filter
  const filterInput = await locatorOne(sceneRoot, '.global-filter > input');
  // Global filter: 202 rows contain 1725, in column Created
  await filterInput.fill('1725');
  // Filtering does not change the selection
  await expectSelectedItemsCount(sceneRoot, 303);
  // Unselect all filtered rows
  await selectAllClick(table);
  await expectSelectedItemsCount(sceneRoot, 101);
  // Select all rows
  await selectAllClick(table);
  // Select all after filtering only selects filtered rows
  await expectSelectedItemsCount(sceneRoot, 303);

  await nextItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 202, 25, 2,
      [38,40,41,43,44,46,47,49,50,52,53,55,56,58,59,61,62,64,65,67,68,70,71,73,74]);
  await expectSelectedItemsCount(sceneRoot, 303);

  // Sort by Created ascending and Idx descending
  await getColumnHeaderLabel(table, 'Created').click();
  await getColumnHeaderLabel(table, 'Idx').click();
  await getColumnHeaderLabel(table, 'Idx').click();
  await expectPaginationState(page, table, paginationDiv, 202, 25, 2,
      [227,224,221,218,215,212,209,206,203,200,197,194,191,188,185,182,179,176,173,170,167,164,161,158,155]);
  await lastItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 202, 25, 9,
      [4,1]);
  await expectSelectedItemsCount(sceneRoot, 303);
  // Clear the filter and deselect all rows
  await filterInput.fill('');
  await selectAllClick(table);
  await expectSelectedItemsCount(sceneRoot, 0);
});


