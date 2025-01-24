import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectShortMsg } from './job-cell';

test('coe-resource-actions-multiple', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'job-cell-scenes', 'coe-resource-actions-multiple');
  const rootLocator = await sceneRoot.locator('#root');
  await expect(sceneRoot.getByRole('button', { name: 'Reset' })).toBeVisible();
  await expect(rootLocator).toHaveScreenshot();
  await sceneRoot.getByRole('button', { name: '▼' }).click();
  await expect(rootLocator).toHaveScreenshot();
  await expect(sceneRoot.getByRole('button', { name: '▲' })).toBeVisible();
  await expect(rootLocator).toHaveScreenshot();
  });
