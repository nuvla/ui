import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectShortMsg } from './job-cell';

test('coe-resource-actions-single-success', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'job-cell-scenes', 'coe-resource-actions-single-success');
  await expect(sceneRoot.getByRole('listitem')).toHaveCount(1);
  await expect(sceneRoot.getByRole('listitem').locator('.label.green')).toHaveText('200');
  await expect(sceneRoot.getByRole('listitem')).toContainText('Image hello-world:latest was already present and updated');
});
