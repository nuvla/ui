import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectShortMsg } from './job-cell';

test('coe-resource-actions-fail-parsing', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'job-cell-scenes', 'coe-resource-actions-missing-docker-entry');
  await expectShortMsg(sceneRoot, '{"msg": "json but not docker key inside rendered also as default');
});
