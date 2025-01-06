import { test, expect } from '@playwright/test';
import { locatorOne } from './utils';

export async function clickAppStoreTab(sceneRoot) {
  const tab = await sceneRoot.locator('.ui.uix-tab-nav .item').nth(0);
  await tab.click();
}

export async function clickAllAppsTab(sceneRoot) {
  const tab = await sceneRoot.locator('.ui.uix-tab-nav .item').nth(1);
  await tab.click();
}

export async function clickMyAppsTab(sceneRoot) {
  const tab = await sceneRoot.locator('.ui.uix-tab-nav .item').nth(2);
  await tab.click();
}

export async function fillTextFilter(sceneRoot, textFilter) {
    const filterInput = await locatorOne(sceneRoot, '.ui.segment.active.tab .ui.input input');
    await filterInput.fill(textFilter);
}

export async function toggleNthModule(sceneRoot, n) {
  const itemDiv = await sceneRoot.locator('.ui.segment.active.tab .list .item').nth(n);
  await itemDiv.click();
}

export async function expectItemsCount(sceneRoot, expectedItemsCount) {
  const items = await sceneRoot.locator('.ui.list .item');
  await expect(items).toHaveCount(expectedItemsCount);
}

export async function expectSelectedCount(sceneRoot, expectedSelectedCount) {
  const selectedCount = await sceneRoot.getByTestId('modules-count');
  await expect(selectedCount).toHaveText('Number of modules selected: ' + expectedSelectedCount);
}

