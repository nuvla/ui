import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { gotoPage } from './pagination';
import {
    clickAppStoreTab,
    clickAllAppsTab,
    clickMyAppsTab,
    expectItemsCount
} from './module-selector';

test('module-selector-pagination', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'module-selector-scenes', 'pagination');
  await clickAllAppsTab(sceneRoot);
  await expectItemsCount(sceneRoot, 4);
  await gotoPage(sceneRoot, 2);
  await expectItemsCount(sceneRoot, 1);
});
