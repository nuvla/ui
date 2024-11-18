import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne, selectOption, selectorOptions } from './utils';

async function expectTotalSeconds(sceneRoot, totalSeconds) {
  await expect(sceneRoot.getByTestId('total-seconds')).toHaveText('Duration in seconds: ' + totalSeconds);
}

test('duration-picker-custom-options', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'duration-picker-scenes', 'custom-options');

  const daysSelector    = await locatorOne(sceneRoot, '.ui.dropdown.duration-days');
  const hoursSelector   = await locatorOne(sceneRoot, '.ui.dropdown.duration-hours');
  const minutesSelector = await locatorOne(sceneRoot, '.ui.dropdown.duration-minutes');
  const secondsSelector = await locatorOne(sceneRoot, '.ui.dropdown.duration-seconds');

  await expect(await selectorOptions(daysSelector)).toEqual(['0', '5', '10']);
  await expect(await selectorOptions(hoursSelector)).toEqual(['0', '5', '10', '15', '20']);
  await expect(await selectorOptions(minutesSelector)).toEqual(['0', '10', '20', '30', '40', '50']);
  await expect(await selectorOptions(secondsSelector)).toEqual(['0', '30']);

  await selectOption(daysSelector, '5');
  await selectOption(hoursSelector, '10');
  await selectOption(minutesSelector, '10');
  await selectOption(secondsSelector, '30');

  await expectTotalSeconds(sceneRoot, 5 * 3600 * 24 + 10 * 3600 + 10 * 60 + 30);
});
