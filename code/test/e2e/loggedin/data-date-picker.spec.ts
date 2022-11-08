import { test, expect } from '@playwright/test';

test('Datepicker test', async ({ page }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  const dataURL = baseURL + '/ui/data';
  await page.goto(baseURL + '/ui/welcome');
  await page.getByRole('link', { name: 'data' }).click();
  await expect(page).toHaveURL(dataURL);

  const date = new Date(new Date().setDate(new Date().getDate() - 30));
  date.setDate(date.getDate() - 1);
  date.setMinutes(10);
  date.setSeconds(0);
  date.setHours(8);

  await page.locator('input[type="text"]').first().click();

  await page.route('/api/data-record', (route) => {
    const payload = route.request().postDataJSON();
    const matches = payload.filter.match(/\d{4}-\d{2}-(\d{2})/);
    expect(Number(matches?.[1]), 'should send correct day').toBe(date.getDate());
    route.fulfill({ status: 200 });
  });

  await Promise.all([
    Promise.race([
      // Format of old version of react-datepicker
      // Remove after react-datepicker is updated
      page.getByRole('option', { name: `day-${date.getDate()}` }).click(),

      // Format of updated react-datepicker
      page.getByRole('option', { name: new RegExp(format(date)) }).click(),
    ]),
    page.waitForResponse('/api/data-record'),
  ]);

  const isoDateString = dateStringRemoveMS(date.toISOString());
  await page.route('/api/data-record', (route) => {
    const payload = route.request().postDataJSON();
    const matches = payload.filter.match(fromDateRegex);
    expect(matches?.[1]).toBe(isoDateString);
    route.fulfill({
      status: 200,
    });
  });

  const newTime = `0${date.getHours()}:${date.getMinutes()}`;

  await Promise.all([page.getByText(newTime).click(), page.waitForResponse('/api/data-record')]);
});

const format = new Intl.DateTimeFormat('en-US', {
  day: 'numeric',
  month: 'long',
  weekday: 'long',
}).format;

const dateStringRemoveMS = (dateString) => {
  return dateString.replace(/\.\d\d\dZ/, 'Z');
};

const fromDateRegex = />='(\d{4}-.*:00Z)' and/;
