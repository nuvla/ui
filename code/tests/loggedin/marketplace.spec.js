import { test, expect } from '@playwright/test';

test.skip('Marketplace shows only published apps', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  const marketplaceUrl = baseURL + '/ui/apps';

  await page.goto(marketplaceUrl);
  // await page.pause();
  // await page.waitForRequest((request) => request.postDataJSON()?.filter?.includes?.('published=true'));
  // await page.waitForResponse((resp) => resp.url().includes('module'));
  // await page.waitForResponse((resp) => resp.url().includes('module'));
  // await page.waitForTimeout(5000);
  // const appCards = await page.waitForSelector('a.ui.card', { state: 'visible' });
  // const appCards = page.locator('a.ui.card');
  const appCards = page.locator('a.ui.card');
  await appCards.nth(0).waitFor();
  // await appCards.click();
  // console.log(page.url());
  // await page.pause();
  // console.log({ appCards });
  const linksCount = await appCards.count();
  console.log({ linksCount });

  // TODO: check that all apps are published
  // for (let i = 0; i < linksCount; i++) {
  //   const hrefs = [];
  //   for (let i = 0; i < linksCount; i++) {
  //     hrefs.push(await appCards.nth(i).getAttribute('href'));
  //   }

  //   console.log(hrefs);
  // }
});
