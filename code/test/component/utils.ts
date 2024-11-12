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

export async function locatorOne(element, selector) {
  const locator = await element.locator(selector);
  await expect(locator).toHaveCount(1);
  return locator.first();
}

export async function dragAndDrop(page, from, to) {
  // regular Playwright dragTo function does not seem to work with "activationConstraint" #js {"distance" 5}
  const toCoords = await to.boundingBox();
  await from.hover();
  await page.mouse.down();
  await page.mouse.move(toCoords.x + toCoords.width / 2, toCoords.y + toCoords.height / 2, {steps: 2});
  await page.mouse.up();
}

export async function selectOption(dropdownDiv, optionLabel) {
  await dropdownDiv.click();
  await dropdownDiv.getByText(new RegExp('^' + optionLabel + '$')).click();
}

export async function selectorOptions(dropdownDiv) {
  await dropdownDiv.click();
  const optionElements = await dropdownDiv.getByRole('option').elementHandles();
  const options = await Promise.all(optionElements.map(async el => { return await el.textContent(); }));
  await dropdownDiv.click();
  return options;
}
