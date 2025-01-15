import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import { gotoPage } from './pagination';
import {
    fillTextFilter,
    expectItemsCount
} from './edge-selector';

test('edge-selector-pagination', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'edge-selector-scenes', 'pagination');
  await fillTextFilter(sceneRoot, 'COMMISSIONED');
  await expectItemsCount(sceneRoot, 4);
  await gotoPage(sceneRoot, 2);
  await expectItemsCount(sceneRoot, 2);
});

