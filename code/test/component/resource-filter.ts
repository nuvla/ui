import { test, expect } from '@playwright/test';
import { locatorOne, selectOption } from './utils';

export async function selectAttribute(resourceFilter, position, attributeName) {
  const attributeDropdown = await resourceFilter.locator('.attr-name-dropdown').nth(position);
  await selectOption(attributeDropdown, attributeName);
}

export async function selectOperation(resourceFilter, position, operation) {
  const operationDropdown = await resourceFilter.locator('.operation-dropdown').nth(position);
  await selectOption(operationDropdown, operation);
}

export async function selectValue(resourceFilter, position, value) {
  const valueField = await resourceFilter.locator('.value-dropdown input').nth(position);
  await valueField.fill(value);
  await valueField.blur();
}

export async function insertElement(resourceFilter, position, elementName) {
  const newElementDropdown = await resourceFilter.locator('.insert-element-dropdown').nth(position);
  await selectOption(newElementDropdown, elementName);
}

export async function openFilterModal(sceneRoot) {
  const modalTriggerButton = await locatorOne(sceneRoot, '.modal-trigger');
  await modalTriggerButton.click();
  return await locatorOne(sceneRoot, '.ui.modal.resource-filter-modal');
}

export async function filterModalDone(modalDiv) {
  const modalDoneButton = await locatorOne(modalDiv, '.done-button');
  await modalDoneButton.click();
}

export async function expectFilterQuery(sceneRoot, expectedFilterQuery) {
  const filterQuery = await sceneRoot.getByTestId('filter-query');
  await expect(filterQuery).toHaveText('Filter query: ' + expectedFilterQuery);
}
