import { test, expect } from '@playwright/test';

test.skip('Logout logs me out', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  const welcomePageUrl = baseURL + '/ui/welcome';
  const signInPageUrl = baseURL + '/ui/sign-in';

  await page.goto(welcomePageUrl);
  await page.getByText(/^logout$/i).click();
  await expect(page).toHaveURL(signInPageUrl);

  await page.goto(welcomePageUrl);

  // Testing redirect to login page when navigating
  await page.getByRole('link', { name: 'Edges' }).click();
  await expect(page).toHaveURL(signInPageUrl);

  await page.goto(welcomePageUrl);
  // Testing existence of working login button
  await page.getByText(/^login$/i).click();
  await expect(page).toHaveURL(signInPageUrl);
});
