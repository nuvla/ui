import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectShortMsg } from './job-cell';

test('coe-resource-actions-multiple', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'job-cell-scenes', 'coe-resource-actions-multiple');
  await expect(sceneRoot).toHaveScreenshot({ timeout: 10000 });
  await sceneRoot.getByRole('button', { name: '▼' }).click();
  await expect(sceneRoot).toHaveScreenshot({ timeout: 10000 });
  await expect(sceneRoot.getByRole('button', { name: '▲' })).toBeVisible();
  await expect(sceneRoot).toHaveScreenshot({ timeout: 10000 });
  });
