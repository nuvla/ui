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

// selectable items locator: exclude project folders
const itemsLocator = '.ui.segment.active.tab .ui.list .item > i.square';

export async function toggleNthModule(sceneRoot, n) {
  const itemDiv = await sceneRoot.locator(itemsLocator).nth(n);
  await itemDiv.click();
}

export async function expectItemsCount(sceneRoot, expectedItemsCount) {
  const items = await sceneRoot.locator(itemsLocator);
  await expect(items).toHaveCount(expectedItemsCount);
}

export async function expectSelectedCount(sceneRoot, expectedSelectedCount) {
  const selectedCount = await sceneRoot.getByTestId('modules-count');
  await expect(selectedCount).toHaveText('Number of modules selected: ' + expectedSelectedCount);
}

