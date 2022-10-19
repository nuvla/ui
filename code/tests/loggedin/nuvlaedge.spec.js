import { test, expect } from '@playwright/test';

test('NuvlaEdge creation and deletion', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.waitForResponse((resp) => resp.url().includes('get-subscription'));
  await page.getByRole('link', { name: 'Edges' }).click();

  const edgesPageRegex = /\/ui\/edges$/;

  await expect(page).toHaveURL(edgesPageRegex);

  await page.getByText('Add').click();

  await page.getByText('bluetooth').click();

  await page.getByText('gpu').click();

  await page.getByText('modbus').click();

  await page.getByText('network').click();

  await page.getByText('usb').first().click();

  await page.getByText('Compose file bundle').click();

  await page.getByText('Enable host-level management').click();

  const newEdgeName = `e2e Tesing: Edge creation and deletion in ${project.name}`;
  await page.locator('input[type="input"]').click();
  await page.locator('input[type="input"]').fill(newEdgeName);

  await page.getByRole('button', { name: 'create' }).click();

  await page.locator('span:has-text("Host-level management") i').nth(1).click();

  await page
    .locator('span:has-text("Enabled and ready to be used. Please copy this cronjob and add it to your system") i')
    .click();

  await page.locator('.close').click();

  await page.getByRole('link', { name: new RegExp(`${newEdgeName}`) }).click();

  await page.getByText('Disable host level management').click();
  await page.getByRole('button', { name: 'Disable Host Level Management' }).click();
  await page.getByText('success executing operation disable-host-level-management').click();

  await page.getByText(/^delete$/i).click();
  await page.getByRole('button', { name: 'delete' }).click();
  await expect(page).toHaveURL(edgesPageRegex);

  // This is a workaround to test clipboard content if cronjob is copied
  // see: https://github.com/microsoft/playwright/issues/8114
  const modifier = process.env.MODIFIER_KEY || 'Control';
  await page.setContent(`<div contenteditable>123</div>`);
  await page.focus('div');
  await page.keyboard.press(`${modifier}+KeyV`);
  await page.keyboard.press(`${modifier}+KeyV`);
  const cronjob = await page.evaluate(() => document.querySelector('div').textContent);
  expect(cronjob.startsWith('* 0 * * * ')).toBeTruthy();
  for (const envVar of ['NUVLABOX_API_KEY', 'NUVLABOX_API_SECRET', 'NUVLA_ENDPOINT']) {
    const testRegex = new RegExp(` (${envVar})=`);
    const [_, matchedEnvVar] = cronjob.match(testRegex) || [];

    expect(matchedEnvVar).toBe(envVar);
  }
});
