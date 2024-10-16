import { test, expect } from '@playwright/test';

export async function expectMsg(sceneRoot, msg) {
    await expect(sceneRoot).toContainText(msg);
}

export async function expectShortMsg(sceneRoot, msg) {
  await expectMsg(sceneRoot, msg);
  await expect(sceneRoot.getByRole('button', { name: '▼' })).toBeHidden();
}
