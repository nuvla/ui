import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    clickAppStoreTab,
    clickAllAppsTab,
    clickMyAppsTab,
    toggleNthModule,
    expectItemsCount,
    expectSelectedCount
} from './module-selector';

async function toggleDockerCompose(sceneRoot) {
  const dockerComposeCheckbox = await sceneRoot.getByTestId('checkbox-docker-compose');
  await dockerComposeCheckbox.click();
}

async function toggleKubernetes(sceneRoot) {
  const kubernetesCheckbox = await sceneRoot.getByTestId('checkbox-kubernetes');
  await kubernetesCheckbox.click();
}

async function toggleHelm(sceneRoot) {
  const helmCheckbox = await sceneRoot.getByTestId('checkbox-helm');
  await helmCheckbox.click();
}

test('module-selector-filter-by-subtype', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'module-selector-scenes', 'filter-by-subtype');
  await clickAllAppsTab(sceneRoot);
  await expectItemsCount(sceneRoot, 5);

  await toggleDockerCompose(sceneRoot);
  await clickAllAppsTab(sceneRoot);
  await expectItemsCount(sceneRoot, 3);

  await toggleKubernetes(sceneRoot);
  await toggleHelm(sceneRoot);
  await expectItemsCount(sceneRoot, 0);
});
