import { test, expect } from '@playwright/test';

export async function expectMsg(sceneRoot, msg) {
    await expect(sceneRoot.getByRole('paragraph')).toContainText(msg);
}

export async function expectShortMsg(sceneRoot, msg) {
  await expectMsg(sceneRoot, msg);
  await expect(sceneRoot.getByText('Show more')).toBeHidden();
}
