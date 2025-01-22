import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    selectAttribute,
    selectOperation,
    selectValue,
    insertElement,
    expectFilterQuery
} from './resource-filter';

test('resource-filter-basic', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'resource-filter-scenes', 'basic');
  const resourceFilter = await locatorOne(sceneRoot, '.resource-filter');

  await expectFilterQuery(sceneRoot, '');

  await selectAttribute(resourceFilter, 0, 'login-username');
  await selectOperation(resourceFilter, 0, 'Equal');
  await selectValue(resourceFilter, 0, 'userA');
  await expectFilterQuery(sceneRoot, 'login-username=\'userA\'');

  await insertElement(resourceFilter, 1, 'AND');
  await insertElement(resourceFilter, 2, 'Attribute');

  await selectAttribute(resourceFilter, 1, 'monitored');
  await selectOperation(resourceFilter, 1, 'is');
  // no changes until clause is complete
  await expectFilterQuery(sceneRoot, 'login-username=\'userA\'');
  await selectValue(resourceFilter, 1, 'true');
  await expectFilterQuery(sceneRoot, 'login-username=\'userA\' and monitored=true');
});
