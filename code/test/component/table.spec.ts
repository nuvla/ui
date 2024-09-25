import { test, expect } from '@playwright/test';
import { gotoScene } from './utils';

test('test', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'table-refactor-scenes', 'table-refactor');

  const table = await sceneRoot.locator('table.ui');
  expect(table).toHaveCount(1);

  expect(table.first().locator('thead tr')).toHaveCount(1);
  expect(table.first().locator('tbody tr')).toHaveCount(2);
});

