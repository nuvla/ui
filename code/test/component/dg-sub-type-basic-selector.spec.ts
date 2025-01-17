import { test, expect } from '@playwright/test';
import { gotoScene, locatorOne } from './utils';

async function expectSelectedDGSubType(sceneRoot, subtype) {
   const selectedSubtypeLabel = await sceneRoot.getByTestId('selected-dg-sub-type');
   await expect(selectedSubtypeLabel).toHaveText('Deployment Group sub type is ' + subtype);
}

test('dg-sub-type-selector', async ({ page }, { config }) => {
  const sceneRoot = await gotoScene(config, page, 'dg-sub-type-selector-scenes', 'basic-selector');
  const dockerSwarmCard = await locatorOne(sceneRoot, '.ui.card.docker-swarm');
  await dockerSwarmCard.click();
  await expectSelectedDGSubType(sceneRoot, 'docker-swarm');
  const kubernetesCard = await locatorOne(sceneRoot, '.ui.card.kubernetes');
  await kubernetesCard.click();
  await expectSelectedDGSubType(sceneRoot, 'kubernetes');
  const dockerComposeCard = await locatorOne(sceneRoot, '.ui.card.docker-compose');
  await dockerComposeCard.click();
  await expectSelectedDGSubType(sceneRoot, 'docker-compose');

  // test disabled component
  const disabledCheckbox = await sceneRoot.getByTestId('checkbox-disabled');
  await disabledCheckbox.click();
  // click has no effect when component is disabled
  await dockerSwarmCard.click();
  await expectSelectedDGSubType(sceneRoot, 'docker-compose');
  // enable back the component
  await disabledCheckbox.click();
  await dockerSwarmCard.click();
  await expectSelectedDGSubType(sceneRoot, 'docker-swarm');
});
