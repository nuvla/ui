import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    clickAppStoreTab,
    clickAllAppsTab,
    clickMyAppsTab,
    toggleNthModule,
    expectSelectedCount
} from './module-selector';

test('module-selector-basic', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'module-selector-scenes', 'basic-selector');
  await expectSelectedCount(sceneRoot, 0);
  await clickAllAppsTab(sceneRoot);
  await toggleNthModule(sceneRoot, 0);
  await toggleNthModule(sceneRoot, 1);
  await expectSelectedCount(sceneRoot, 2);
  await toggleNthModule(sceneRoot, 2);
  await toggleNthModule(sceneRoot, 3);
  await toggleNthModule(sceneRoot, 4);
  await expectSelectedCount(sceneRoot, 5);
  await toggleNthModule(sceneRoot, 3);
  await toggleNthModule(sceneRoot, 4);
  await expectSelectedCount(sceneRoot, 3);
});

