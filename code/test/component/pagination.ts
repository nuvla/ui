import { test, expect } from '@playwright/test';
import { locatorOne } from './utils';

export async function selectPageSize(sceneRoot, pageSize) {
  const paginationDiv = await locatorOne(sceneRoot, '.uix-pagination-control');
  const paginationSummary = await locatorOne(paginationDiv, '>div:nth-child(1)');
  const pageSizeSelection = await locatorOne(paginationDiv, '>div:nth-child(3)');
  await pageSizeSelection.click();
  await pageSizeSelection.getByText(pageSize.toString()).click();
}

export async function gotoPage(sceneRoot, page) {
  const paginationMenu = await locatorOne(sceneRoot, 'div.ui.pagination.menu');
  const paginationMenuItems = await paginationMenu.locator('a[type=pageItem].item');
  const menuItemsCount = await paginationMenuItems.count();
  for (let i = 0 ; i < menuItemsCount ; i++) {
      const menuItem = paginationMenuItems.nth(i);
      const menuItemText = await menuItem.textContent();
      if (menuItemText == page.toString()) {
          await menuItem.click();
          return;
      }
  }
  throw new Error("page not found in pagination menu");
}

export async function expectPaginationMenuItems(sceneRoot, itemsText) {
  const paginationMenu = await locatorOne(sceneRoot, 'div.ui.pagination.menu');
  const paginationMenuItems = await paginationMenu.locator('a[type=pageItem].item');
  await expect(paginationMenuItems).toHaveCount(itemsText.length);
  for (let i = 0 ; i < itemsText.length ; i++) {
      await expect(paginationMenuItems.nth(i)).toHaveText(itemsText[i].toString());
  }
}
