import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { selectPageSize, expectPaginationMenuItems } from './pagination';

async function setNumberOfItems(sceneRoot, totalItems) {
  const nItemsInput = await locatorOne(sceneRoot, '.total-items-input > input');
  await nItemsInput.fill(totalItems.toString());
}

test('pagination-basic', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'pagination-scenes', 'basic-pagination');

  await expectPaginationMenuItems(sceneRoot, [1, 2, 3, 4, 10]);
  await setNumberOfItems(sceneRoot, 15);
  await expectPaginationMenuItems(sceneRoot, [1, 2]);
  await setNumberOfItems(sceneRoot, 200);
  await expectPaginationMenuItems(sceneRoot, [1, 2, 3, 4, 20]);
  await selectPageSize(sceneRoot, 25);
  await setNumberOfItems(sceneRoot, 1000);
  await expectPaginationMenuItems(sceneRoot, [1, 2, 3, 4, 40]);
});
