import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectMsg } from './job-cell';

test('monitored-job-without-progress', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'bulk-progress-monitored-job-scenes', 'monitored-job-without-progress');
  await expect(sceneRoot.getByRole('button', { name: 'Reset' })).toBeVisible();
  await expect(sceneRoot).toHaveScreenshot();
  await sceneRoot.getByRole('cell', { name: 'Offline Edges' }).locator('div').first().click();
  await expect(sceneRoot).toHaveScreenshot();
  await sceneRoot.getByRole('cell', { name: 'Error reason foobar' }).locator('div').first().click();
  await expect(sceneRoot).toHaveScreenshot();
});
