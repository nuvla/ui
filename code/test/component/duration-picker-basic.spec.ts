import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne, selectOption } from './utils';

async function expectTotalSeconds(sceneRoot, totalSeconds) {
  await expect(sceneRoot.getByTestId('total-seconds')).toHaveText('Duration in seconds: ' + totalSeconds);
}

test('duration-picker-basic', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'duration-picker-scenes', 'basic-picker');

  const daysSelector    = await locatorOne(sceneRoot, '.ui.dropdown.duration-days');
  const hoursSelector   = await locatorOne(sceneRoot, '.ui.dropdown.duration-hours');
  const minutesSelector = await locatorOne(sceneRoot, '.ui.dropdown.duration-minutes');
  const secondsSelector = await locatorOne(sceneRoot, '.ui.dropdown.duration-seconds');

  await selectOption(daysSelector, '4');
  await selectOption(hoursSelector, '10');
  await selectOption(minutesSelector, '10');
  await selectOption(secondsSelector, '25');

  await expectTotalSeconds(sceneRoot, 4 * 3600 * 24 + 10 * 3600 + 10 * 60 + 25);
});

