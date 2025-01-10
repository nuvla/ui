import { test, expect } from '@playwright/test';
import { locatorOne } from './utils';

export async function fillTextFilter(sceneRoot, textFilter) {
    const filterInput = await locatorOne(sceneRoot, '.ui.segment .ui.input input');
    await filterInput.fill(textFilter);
}

export async function toggleNthEdge(sceneRoot, n) {
  const itemRow = await sceneRoot.locator('.ui.table tbody tr').nth(n);
  const itemCheckbox = await locatorOne(itemRow, 'div.checkbox');
  await itemCheckbox.click();
}

export async function expectItemsCount(sceneRoot, expectedItemsCount) {
  const items = await sceneRoot.locator('.ui.table tbody tr');
  await expect(items).toHaveCount(expectedItemsCount);
}

export async function expectSelectedCount(sceneRoot, expectedSelectedCount) {
  const selectedCount = await sceneRoot.getByTestId('edges-count');
  await expect(selectedCount).toHaveText('Number of edges selected: ' + expectedSelectedCount);
}
