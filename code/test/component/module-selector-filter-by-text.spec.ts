import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    clickAppStoreTab,
    clickAllAppsTab,
    clickMyAppsTab,
    fillTextFilter,
    expectItemsCount
} from './module-selector';

test('module-selector-filter-by-text', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'module-selector-scenes', 'basic-selector');
  await expectItemsCount(sceneRoot, 2);
  await fillTextFilter(sceneRoot, 'app 0');
  await expectItemsCount(sceneRoot, 1);
});

