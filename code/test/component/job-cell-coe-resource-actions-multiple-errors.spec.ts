import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';
import { expectShortMsg } from './job-cell';

test('coe-resource-actions-multiple-errors', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'job-cell-scenes', 'coe-resource-actions-multiple-errors');
  await expect(sceneRoot.getByText('Operation not supported for network -- 085f8229d6dd9b9e7e99513bfd6c20f35c75057382f6d5c119e1d29a1172c667')).toBeVisible();
  await expect(sceneRoot.getByText('Operation not supported for network -- cd4adbc05d7daa22be710a7c2e43a7242d9e212a5be66fb917261dd387911f5f')).toBeVisible();
  await expect(sceneRoot.getByText('Unknown error: {"message":"rpc error: code = FailedPrecondition desc = ingress')).toBeVisible();
  await expect(sceneRoot.getByRole('listitem')).toHaveCount(6);
  await expect(sceneRoot.getByRole('listitem').nth(0)).toBeVisible();
  await expect(sceneRoot.getByRole('listitem').nth(0).locator('.label.red')).toHaveText('403');
  await expect(sceneRoot.getByRole('listitem').nth(0)).toBeInViewport();
  await expect(sceneRoot.getByRole('listitem').nth(2)).toBeVisible();
  await expect(sceneRoot.getByRole('listitem').nth(2).locator('.label.red')).toHaveText('400');
  await expect(sceneRoot.getByRole('listitem').nth(2)).toBeInViewport();
  await expect(sceneRoot.getByRole('listitem').nth(3)).toBeVisible();
  await expect(sceneRoot.getByRole('listitem').nth(3).locator('.label.red')).toHaveText('403');
  await expect(sceneRoot.getByRole('listitem').nth(3)).toBeInViewport();
  await expect(sceneRoot.getByRole('listitem').nth(4)).toBeVisible();
  await expect(sceneRoot.getByRole('listitem').nth(4)).not.toBeInViewport();
  await expect(sceneRoot.getByRole('listitem').nth(5)).toBeVisible();
  await expect(sceneRoot.getByRole('listitem').nth(5)).not.toBeInViewport();
  await sceneRoot.getByRole('button', { name: '▼' }).click();
  await expect(sceneRoot.getByRole('button', { name: '▲' })).toBeVisible();
  await expect(sceneRoot.getByRole('listitem').nth(5)).toBeInViewport();
  await expect(sceneRoot.getByRole('listitem').nth(5).locator('.label.red')).toHaveText('400');
  await expect(sceneRoot.getByText('Unknown error: {"message":"rpc error: code = FailedPrecondition desc = network')).toBeVisible();
});
