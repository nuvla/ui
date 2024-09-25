import { expect } from '@playwright/test';

export async function gotoScene(config, page, componentNamespace, scene) {
  const { baseURL } = config.projects[0].use;
  const url = baseURL + '/?id=sixsq.nuvla.ui.components.' + componentNamespace + '%2F' + scene;
  await page.goto(url);
  await page.waitForURL(url);
  const portfolioIframe = await page.frameLocator('iframe.canvas').first();
  expect(portfolioIframe).not.toBeNull();
  const sceneIframe = await portfolioIframe.frameLocator('iframe.scene-canvas').first();
  expect(sceneIframe).not.toBeNull();
  const sceneRootLocator = await sceneIframe.locator('body');
  expect(sceneRootLocator).toHaveCount(1);
  return sceneRootLocator.first();
}


