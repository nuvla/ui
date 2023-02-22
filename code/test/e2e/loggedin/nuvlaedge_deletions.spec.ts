import { test, expect } from '@playwright/test';

// e2e edges deletion script
test('Deletes all nuvlaedges created through e2e tests', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'Edges' }).click();

  await page.pause();
  const edgesPageRegex = /\/ui\/edges/;

  await expect(page).toHaveURL(edgesPageRegex);
  await page.getByText('25 per page').click();
  await page.getByRole('option', { name: '100' }).getByText('100').click();

  const newEdgeName = `e2e Testing: Edge creation and deletion in`;
  await page.getByPlaceholder('Search ...').click();
  page.getByPlaceholder('Search ...').fill(newEdgeName);
  await page.waitForResponse('/api/nuvlabox');
  await page.waitForTimeout(1000);

  let found = await page.getByRole('link', { name: new RegExp(newEdgeName) }).count();
  while (found > 0) {
    await page
      .getByRole('link', { name: new RegExp(newEdgeName) })
      .nth(0)
      .click({ timeout: 5000 });
    await page.locator('a:has-text("delete")').click();
    // await page.pause();
    await page.getByRole('button', { name: 'delete' }).click();
    await page.waitForURL('https://nui.localhost/ui/edges?view=table');
    await page.getByPlaceholder('Search ...').click();
    page.getByPlaceholder('Search ...').fill(newEdgeName);
    await page.waitForResponse('/api/nuvlabox');
    await page.waitForTimeout(500);
    // await page.pause();
    found = await page.getByRole('link', { name: new RegExp(newEdgeName) }).count();
  }
});
