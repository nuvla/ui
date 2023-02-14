import { test, expect } from '@playwright/test';

test('test', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');

  await page.getByText('We enable your edge, as a ServiceDeploy any containerised app, to the edge, Kube').click();

  // Deployments aliases
  await page.getByRole('link').getByText('Deployments').click();
  await expect(page).toHaveURL(`${baseURL}/ui/deployments`);
  await page.getByRole('heading').getByText('Deployments').isVisible();

  await page.goto(`${baseURL}/ui/deployment`);
  await expect(page).toHaveURL(`${baseURL}/ui/deployment`);
  await page.getByRole('heading').getByText('Deployments').isVisible();

  // Edges aliases
  await page.getByRole('link').getByText('Edges').click();
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/edges`));
  await page.getByRole('heading').getByText('Edges').isVisible();

  await page.goto(`${baseURL}/ui/edge`);
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/edge`));
  await page.getByRole('heading').getByText('Edges').isVisible();

  await page.goto(`${baseURL}/ui/nuvlabox`);
  await expect(page).toHaveURL(new RegExp(`${baseURL}/ui/nuvlabox`));
  await page.getByRole('heading').getByText('Edges').isVisible();

  const edgeName = /e2e-Test-Do_not_delete/;
  await page.getByPlaceholder('Search ...').click();
  await page.getByPlaceholder('Search ...').fill('15339469');
  await page.getByRole('link', { name: edgeName }).click();

  await page.waitForURL(`${baseURL}/ui/edges/15339469-0b4c-4c45-a0b7-65c494e99a3b`);
  await page.getByRole('heading', { name: edgeName }).isVisible();

  await page.goto(`${baseURL}/ui/edge/15339469-0b4c-4c45-a0b7-65c494e99a3b`);
  await expect(page).toHaveURL(`${baseURL}/ui/edge/15339469-0b4c-4c45-a0b7-65c494e99a3b`);
  await page.getByRole('heading', { name: edgeName }).isVisible();

  await page.goto(`${baseURL}/ui/nuvlabox/15339469-0b4c-4c45-a0b7-65c494e99a3b`);
  await expect(page).toHaveURL(`${baseURL}/ui/nuvlabox/15339469-0b4c-4c45-a0b7-65c494e99a3b`);
  await page.getByRole('heading', { name: edgeName }).isVisible();

  // Clouds aliases
  await page.getByRole('link', { name: 'clouds' }).click();
  await expect(page).toHaveURL(`${baseURL}/ui/clouds`);
  await page.getByRole('heading').getByText('Clouds').isVisible();
  await page.goto(`${baseURL}/ui/infrastructures`);
  await expect(page).toHaveURL(`${baseURL}/ui/infrastructures`);
  await page.getByRole('heading').getByText('Clouds').isVisible();

  await page.getByRole('link', { name: /Exoscale S3 - Frankfurt/ }).click();
  await expect(page).toHaveURL(`${baseURL}/ui/clouds/09b08b49-2408-4b80-a7cc-73f420903fd5`);
  await page.goto(`${baseURL}/ui/infrastructures/09b08b49-2408-4b80-a7cc-73f420903fd5`);
  await expect(page).toHaveURL(`${baseURL}/ui/infrastructures/09b08b49-2408-4b80-a7cc-73f420903fd5`);

  await page.goto(`${baseURL}/ui/infrastructures/09b08b49-2408-4b80-a7cc-73f420903fd5`);
});
