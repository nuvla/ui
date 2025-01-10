import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    toggleNthEdge,
    expectSelectedCount
} from './edge-selector';

test('edge-selector-basic', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'edge-selector-scenes', 'basic-selector');
  await expectSelectedCount(sceneRoot, 0);
  await toggleNthEdge(sceneRoot, 0);
  await toggleNthEdge(sceneRoot, 1);
  await expectSelectedCount(sceneRoot, 2);
  await toggleNthEdge(sceneRoot, 2);
  await toggleNthEdge(sceneRoot, 3);
  await toggleNthEdge(sceneRoot, 4);
  await expectSelectedCount(sceneRoot, 5);
  await toggleNthEdge(sceneRoot, 3);
  await toggleNthEdge(sceneRoot, 4);
  await expectSelectedCount(sceneRoot, 3);
});
