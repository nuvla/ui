import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectShortMsg } from './job-cell';

test('coe-resource-actions-fail-parsing', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'job-cell-scenes', 'coe-resource-actions-fail-parsing');
  await expectShortMsg(sceneRoot, 'status message that fail to be parsed as json should be rendered as default');
});
