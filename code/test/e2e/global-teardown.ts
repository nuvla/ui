import { test, expect } from '@playwright/test';

test('teardown', async ({ page}) => {

  const welcomePageUrl = process.env.UI_BASE_URL + '/ui/welcome';
  const signInPageUrl = process.env.UI_BASE_URL + '/ui/sign-in';

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