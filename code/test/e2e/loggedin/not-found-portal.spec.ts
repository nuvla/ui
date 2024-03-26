import { test, expect } from '@playwright/test';

test('Not found portal for nuvlaedge', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;
  for (const {page_url, msg} of [{page_url: 'edges', msg: "Oops can't find NuvlaEdge"},
                                 {page_url: 'apps', msg: "Oops can't find Application"},
                                 {page_url: 'deployments', msg: "Oops can't find deployment"},
                                 {page_url: 'clouds', msg: "Oops can't find service"},]) {
    await page.goto(baseURL + '/ui/' + page_url + '/do-not-exist');
    await expect(page.getByText(msg)).toBeVisible();
  }}
);
