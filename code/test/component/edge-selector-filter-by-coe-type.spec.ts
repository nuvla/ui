import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';
import {
    toggleNthEdge,
    expectItemsCount,
    expectSelectedCount
} from './edge-selector';

async function toggleDocker(sceneRoot) {
  const dockerCheckbox = await sceneRoot.getByTestId('checkbox-docker');
  await dockerCheckbox.click();
}

async function toggleSwarm(sceneRoot) {
  const swarmCheckbox = await sceneRoot.getByTestId('checkbox-swarm');
  await swarmCheckbox.click();
}

async function toggleKubernetes(sceneRoot) {
  const kubernetesCheckbox = await sceneRoot.getByTestId('checkbox-kubernetes');
  await kubernetesCheckbox.click();
}

test('edge-selector-filter-by-subtype', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'edge-selector-scenes', 'filter-by-coe-type');
  await expectItemsCount(sceneRoot, 8);

  await toggleDocker(sceneRoot);
  await expectItemsCount(sceneRoot, 5);

  await toggleSwarm(sceneRoot);
  await expectItemsCount(sceneRoot, 3);

  await toggleKubernetes(sceneRoot);
  await expectItemsCount(sceneRoot, 0);
});

