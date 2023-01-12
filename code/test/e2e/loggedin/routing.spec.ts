import { test, expect } from '@playwright/test';

test('test', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.getByText('We enable your edge, as a ServiceDeploy any containerised app, to the edge, Kube').click();

  await page
    .locator('body:has-text("HomeDashboardAppsDeploymentsDeployment setsEdgesCredentialsNotificationsDataClou")')
    .press('Control+h');

  await page.getByRole('link', { name: 'Create a NuvlaEdge' }).scrollIntoViewIfNeeded();
  await page.getByRole('link', { name: 'Create a NuvlaEdge' }).click();
  await expect(page).toHaveURL('https://nui.localhost/#add-nuvlabox');

  await page.getByRole('heading', { name: 'Create and configure your first NuvlaEdge' }).scrollIntoViewIfNeeded();
  await page.getByRole('heading', { name: 'Create and configure your first NuvlaEdge' }).click();

  await page.getByRole('link', { name: 'Launch an app' }).click();
  await expect(page).toHaveURL('https://nui.localhost/#launch-app');

  await page.getByRole('heading', { name: 'Launch any containerised app' }).click();

  await page.getByRole('link', { name: 'home' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/welcome');

  await page.getByRole('link', { name: 'dashboard' }).click();

  await expect(page).toHaveURL('https://nui.localhost/ui/dashboard');

  await page.getByRole('link', { name: 'apps' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/apps');

  await page.getByRole('link', { name: 'deployments' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/deployments');

  await page.getByRole('link', { name: 'Deployment sets' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/deployment-sets');

  await page.getByRole('link', { name: 'Edges' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edges');

  await page.getByRole('link', { name: 'Credentials' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/credentials');

  await page.getByRole('link', { name: 'notifications' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/notifications');

  await page.getByRole('link', { name: 'data' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/data');

  await page.getByRole('link', { name: 'clouds' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/clouds');

  await page.getByRole('link', { name: 'api' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/api');

  await page.getByRole('link', { name: 'home' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/welcome');

  await page.getByRole('link', { name: 'Edges' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edges');

  await page.pause();
  // TODO: test this here
  await page.getByRole('link', { name: 'test' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edges/12995f00-039d-4a92-b2be-5cf758ae1317');

  await page.getByRole('link', { name: 'Edges' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edges');

  await page.getByRole('link', { name: 'Credentials' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/credentials');

  await page.getByRole('link', { name: 'notifications' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/notifications');

  await page.getByRole('link', { name: 'data' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/data');

  await page.getByRole('link', { name: 'clouds' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/clouds');

  await page.getByRole('link', { name: 'deployments' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/deployments');

  await page.locator('.value').first().click();

  await page.locator('div:nth-child(2) > .value').click();

  await page.getByText('STARTING').click();

  await page.getByText('STOPPED').click();

  await page.getByText('ERROR').click();

  await page.goto('https://nui.localhost/ui/deployment');

  await page.getByRole('link', { name: 'Edges' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edges');

  await page.getByRole('link', { name: 'test 16 days ago endtoend@sixsq.com 30s 2.y.z' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edge/12995f00-039d-4a92-b2be-5cf758ae1317');

  await page.goto('https://nui.localhost/ui/nuvlabox12995f00-039d-4a92-b2be-5cf758ae1317');

  await page.goto('https://nui.localhost/ui/nuvlabox/12995f00-039d-4a92-b2be-5cf758ae1317');

  await page.getByRole('banner').getByText('Edges').click();
  await expect(page).toHaveURL('https://nui.localhost/ui/edges');

  await page.locator('a:has-text("endtoend@sixsq.com")').click();
  await expect(page).toHaveURL('https://nui.localhost/ui/profile');

  await page.getByRole('link', { name: 'clouds' }).click();
  await expect(page).toHaveURL('https://nui.localhost/ui/clouds');

  await page
    .getByRole('link', { name: 'Exoscale S3 - Frankfurt (de-fra-1) Exoscale S3 - Frankfurt (de-fra-1)' })
    .click();
  await expect(page).toHaveURL('https://nui.localhost/ui/clouds/09b08b49-2408-4b80-a7cc-73f420903fd5');

  await page.goto('https://nui.localhost/ui/infrastructures/09b08b49-2408-4b80-a7cc-73f420903fd5');
});
