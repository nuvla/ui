import { test, expect } from '@playwright/test';

// e2e edges deletion script
test('Deletes all nuvlaedges created through e2e tests', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'Edges' }).click();

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
    await page.getByRole('button', { name: 'Delete NuvlaEdge' }).click();
    await page.getByRole('button', { name: 'Yes: Delete NuvlaEdge' }).click();
    await page.waitForURL(`${baseURL}/ui/edges?view=table`);
    await page.getByPlaceholder('Search ...').click();
    page.getByPlaceholder('Search ...').fill(newEdgeName);
    await page.waitForResponse('/api/nuvlabox');
    await page.waitForTimeout(500);
    found = await page.getByRole('link', { name: new RegExp(newEdgeName) }).count();
  }
});

test('logout', async ({ page }) => {

  const welcomePageUrl = process.env.UI_BASE_URL + '/ui/welcome';
  const signInPageUrl = process.env.UI_BASE_URL + '/ui/sign-in';

  await page.goto(welcomePageUrl);
  await page.getByText(/^logout$/i).click();
  await expect(page).toHaveURL(welcomePageUrl);

  // Testing redirect to login page when navigating
  await page.getByRole('link', { name: 'Edges' }).click();
  await expect(page).toHaveURL(new RegExp(`${signInPageUrl}\\?redirect=edges(&view=table)?`));

  await page.goto(welcomePageUrl);
  // Testing existence of working login button
  await page.getByText(/^login$/i).click();
  await expect(page).toHaveURL(signInPageUrl);
});