import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne, dragAndDrop } from './utils';

export async function expectHeadersOrder(table, headers) {
  await expect(table.locator('thead tr th')).toHaveCount(headers.length);
  for (let index = 0 ; index < headers.length ; index++) {
      let header = headers[index];
      await expect(table.locator('thead tr th').nth(index)).toHaveText(header);
  }
}

export async function getColumnIndex(table, headerText) {
  const colCount = await table.locator('thead tr th').count();
  for (let index = 0 ; index < colCount ; index++) {
      const actualHeaderText = await table.locator('thead tr th').nth(index).textContent();
      if (actualHeaderText == headerText) {
        return index;
      }
  }
  return -1;
}

export async function expectColumnData(page, table, colName, colData) {
  const colIndex = await getColumnIndex(table, colName);
  for (let rowIndex = 0; rowIndex < colData.length ; rowIndex++) {
      let cellData = colData[rowIndex];
      await expect(await table.locator('tbody > tr:nth-child(' + (rowIndex + 1) + ') > td:nth-child(' + (colIndex + 1) + ')')).toHaveText(cellData);
  };
}

export async function expectHeaderRowCount(table, rowCount) {
  await expect(table.locator('thead tr')).toHaveCount(rowCount);
}

export async function expectTableRowCount(table, rowCount) {
  await expect(table.locator('tbody tr')).toHaveCount(rowCount);
}

export async function openColumnSelectorModal(sceneRoot) {
  return sceneRoot.getByTitle('Columns selector').locator('i').click();
}

export function getColumnHeader(table, colName) {
  return table.getByRole('button', { name: colName });
}

export function getColumnHeaderLabel(table, colName) {
  return getColumnHeader(table, colName).getByTestId('column-header-text');
}

export function getDeleteColumnButton(table, colName) {
  return getColumnHeader(table, colName).locator('a[aria-label="Delete Column"]');
}

export async function selectAllClick(table) {
  await table.locator('thead > tr > th:nth-child(1) > div').click();
}

export async function selectRow(table, rowIndex) {
  await table.locator('tbody > tr:nth-child(' + (rowIndex + 1) + ') > td:nth-child(1) > div').click();
}

export async function expectSelectedItemsCount(sceneRoot, selectedItemsCount) {
  await expect(sceneRoot.getByTestId('selected-items-summary')).toHaveText(selectedItemsCount + ' items selected');
}

export async function expectPaginationState(page, table, paginationDiv, totalItems, pageSize, activeIndex, expectedIdxColumnData) {
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
  await expectColumnData(page, table, 'Idx',
      (expectedIdxColumnData ? expectedIdxColumnData.map(x => x.toString()) : null) ||
      [...Array(expectedRowCount).keys()].map(x => x + pageSize * (activeIndex - 1)).map(x => x.toString())
  );
}

export async function selectPageSize(paginationDiv, pageSize) {
  const paginationSummary = await locatorOne(paginationDiv, '>div:nth-child(1)');
  const pageSizeSelection = await locatorOne(paginationSummary, '>div:nth-child(3)');
  await pageSizeSelection.click();
  await pageSizeSelection.getByText(pageSize.toString()).click();
}
