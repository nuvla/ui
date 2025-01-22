import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    openFilterModal,
    selectAttribute,
    selectOperation,
    selectValue,
    insertElement,
    filterModalDone,
    expectFilterQuery
} from './resource-filter';

test('resource-filter-modal', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'resource-filter-scenes', 'modal');

  const modalDiv = await openFilterModal(sceneRoot);
  const resourceFilter = await locatorOne(modalDiv, '.resource-filter');

  await expectFilterQuery(sceneRoot, '');

  await selectAttribute(resourceFilter, 0, 'login-username');
  await selectOperation(resourceFilter, 0, 'Equal');
  await selectValue(resourceFilter, 0, 'userA');
  // need to click on Done
  await expectFilterQuery(sceneRoot, '');
  // click on Done and check again
  await filterModalDone(modalDiv);
  await expectFilterQuery(sceneRoot, 'login-username=\'userA\'');

  await openFilterModal(sceneRoot);
  await insertElement(resourceFilter, 1, 'AND');
  await insertElement(resourceFilter, 2, 'Attribute');

  await selectAttribute(resourceFilter, 1, 'monitored');
  await selectOperation(resourceFilter, 1, 'is');
  // no changes until clause is complete
  await expectFilterQuery(sceneRoot, 'login-username=\'userA\'');
  await selectValue(resourceFilter, 1, 'true');
  await filterModalDone(modalDiv);
  await expectFilterQuery(sceneRoot, 'login-username=\'userA\' and monitored=true');
});
