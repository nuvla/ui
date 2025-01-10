import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    fillTextFilter,
    expectItemsCount
} from './edge-selector';

test('edge-selector-filter-by-text', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'edge-selector-scenes', 'basic-selector');
  await expectItemsCount(sceneRoot, 8);
  await fillTextFilter(sceneRoot, 'nb-4');
  await expectItemsCount(sceneRoot, 1);
});
