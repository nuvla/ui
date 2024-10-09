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

async function expectHeaderRowCount(table, rowCount) {
  await expect(table.locator('thead tr')).toHaveCount(rowCount);
}

async function expectTableRowCount(table, rowCount) {
  await expect(table.locator('tbody tr')).toHaveCount(rowCount);
}

async function openColumnSelectorModal(sceneRoot) {
  return sceneRoot.getByTitle('Columns selector').locator('i').click();
}

function getColumnHeader(table, colName) {
  return table.getByRole('button', { name: colName });
}

function getColumnHeaderLabel(table, colName) {
  return getColumnHeader(table, colName).getByTestId('column-header-text');
}

function getDeleteColumnButton(table, colName) {
  return getColumnHeader(table, colName).locator('a[aria-label="Delete Column"]');
}

test('test basic table', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'basic-table');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectHeadersOrder(table, ['Id', 'Size', 'Created']);

  expectHeaderRowCount(table, 1);
  expectTableRowCount(table, 3);
});

test('test column customization', async ({ page }, { config }) => {
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

test('test draggable columns', async ({ page }, { config }) => {
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


test('test selectable table rows', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'row-selection');

  const table = await locatorOne(sceneRoot, 'table.ui');
  await expectHeadersOrder(table, ['', 'Id', 'Size', 'Created']);

  // select all
  await table.locator('thead > tr > th:nth-child(1) > div').click();
  await expect(await table.locator('thead > tr > th:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(1) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(3) > td:nth-child(1) > div > input')).toBeChecked();

  // unselect all
  await table.locator('thead > tr > th:nth-child(1) > div').click();
  await expect(await table.locator('tbody > tr:nth-child(1) > td:nth-child(1) > div > input')).toBeChecked({checked: false});
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked({checked: false});
  await expect(await table.locator('tbody > tr:nth-child(3) > td:nth-child(1) > div > input')).toBeChecked({checked: false});

  // select first row
  await table.locator('tbody > tr:nth-child(1) > td:nth-child(1) > div').click();
  await expect(await table.locator('tbody > tr:nth-child(1) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked({checked: false});

  //select second and third rows
  await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div').click();
  await table.locator('tbody > tr:nth-child(3) > td:nth-child(1) > div').click();
  await expect(await table.locator('tbody > tr:nth-child(2) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('tbody > tr:nth-child(3) > td:nth-child(1) > div > input')).toBeChecked();
  await expect(await table.locator('thead > tr > th:nth-child(1) > div > input')).toBeChecked();
});

test('test global filter', async ({ page }, { config }) => {
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

test('test sorting', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'sorting');

  const table = await locatorOne(sceneRoot, 'table.ui');

  await expectHeadersOrder(table, ['Id', 'Size', 'Created', 'Not sortable']);

  expect(table.locator('thead tr')).toHaveCount(1);
  expectTableRowCount(table, 3);

  // Sorted by default by Created ascending by scene definition
  await expectColumnData(page, table, 3, ['1725666894','1725667915','1726074087']);

  // Sort by Created descending
  await getColumnHeaderLabel(table, 'Created').click();
  await expectColumnData(page, table, 3, ['1726074087','1725667915','1725666894']);

  // Sort by Created descending + Id ascending
  await getColumnHeaderLabel(table, 'Id').click();
  await expectColumnData(page, table, 3, ['1726074087','1725667915','1725666894']);

  // No more sorting on Created => Sort by Id ascending
  await getColumnHeaderLabel(table, 'Created').click();
  await expectColumnData(page, table, 3, ['1725666894','1726074087','1725667915']);

  // Disable sorting => data shown in the order in which it is passed in
  await sceneRoot.getByTestId('checkbox-enable-sorting').click();
  await expectColumnData(page, table, 3, ['1726074087','1725667915','1725666894']);

  // Try to sort by Id descending, but expect no effect as sorting is not enabled
  await getColumnHeaderLabel(table, 'Id').click();
  await expectColumnData(page, table, 3, ['1726074087','1725667915','1725666894']);

  // Enable sorting again and then sort by Id descending
  await sceneRoot.getByTestId('checkbox-enable-sorting').click();
  await expectColumnData(page, table, 3, ['1725666894','1726074087','1725667915']);
  await getColumnHeaderLabel(table, 'Id').click();
  await expectColumnData(page, table, 3, ['1725667915','1726074087','1725666894']);
});

async function expectPaginationState(page, table, paginationDiv, totalItems, pageSize, activeIndex, expectedIdxColumnData) {
  const paginationSummary = await locatorOne(paginationDiv, '>div:nth-child(1)');
  const paginationTotal = await locatorOne(paginationSummary, '>div:nth-child(1)');
  const pageSizeSelection = await locatorOne(paginationSummary, '>div:nth-child(3)');
  const pageSizeSelected = await locatorOne(pageSizeSelection, '>div:nth-child(1)');
  const paginationNavigation = await locatorOne(paginationDiv, '>div.uix-pagination-navigation');
  const expectedRowCount = activeIndex * pageSize < totalItems ? pageSize : totalItems % pageSize;

  // Check the text about the total number of items
  await expect(paginationTotal).toHaveText('Total:' + totalItems);
  // Check the selected number of items per page
  await expect(pageSizeSelected).toHaveText(pageSize + ' per page');

  // Expect the current page link to be active
  const activeItemLink = await locatorOne(paginationNavigation, 'a.active[type="pageItem"]');
  await expect(activeItemLink).toHaveText(activeIndex.toString());

  // Check the table row count and Idx column data
  await expectTableRowCount(table, expectedRowCount);
  await expectColumnData(page, table, 4,
      (expectedIdxColumnData ? expectedIdxColumnData.map(x => x.toString()) : null) ||
      [...Array(expectedRowCount).keys()].map(x => x + pageSize * (activeIndex - 1)).map(x => x.toString())
  );
}

async function selectPageSize(paginationDiv, pageSize) {
  const paginationSummary = await locatorOne(paginationDiv, '>div:nth-child(1)');
  const pageSizeSelection = await locatorOne(paginationSummary, '>div:nth-child(3)');
  await pageSizeSelection.click();
  await pageSizeSelection.getByText(pageSize.toString()).click();
}

test('test pagination', async ({ page }, { config }) => {
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

test('test simultaneous filtering, sorting and pagination', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'filter-sort-paginate');
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

  // Apply a global filter
  const filterInput = await locatorOne(sceneRoot, '.global-filter > input');
  // global filter: 202 rows contain 1725, in column Created
  await filterInput.fill('1725');
  await nextItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 202, 25, 2,
      [38,40,41,43,44,46,47,49,50,52,53,55,56,58,59,61,62,64,65,67,68,70,71,73,74]);
  // Sort by Created ascending and Idx descending
  await getColumnHeaderLabel(table, 'Created').click();
  await getColumnHeaderLabel(table, 'Idx').click();
  await getColumnHeaderLabel(table, 'Idx').click();
  await expectPaginationState(page, table, paginationDiv, 202, 25, 2,
      [227,224,221,218,215,212,209,206,203,200,197,194,191,188,185,182,179,176,173,170,167,164,161,158,155]);
  await lastItemLink.click();
  await expectPaginationState(page, table, paginationDiv, 202, 25, 9,
      [4,1]);
});
