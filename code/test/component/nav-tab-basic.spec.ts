import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';

test('nav-tab-basic', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'nav-tab-scenes', 'basic-nav-tab');

  const nav = await locatorOne(sceneRoot, 'div.ui.uix-tab-nav');
  const content = await locatorOne(sceneRoot, 'div.ui.bottom.segment');
  await expect(content).toHaveText('Tab 0 content');
  const tab3 = await nav.locator('a.item').nth(3);
  await tab3.click();
  await expect(content).toHaveText('Tab 3 content');
});
