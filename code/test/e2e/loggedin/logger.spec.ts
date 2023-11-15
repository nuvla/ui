import { test, expect, Page } from '@playwright/test';
import fs from 'fs';

test.use({
  viewport: { width: 1200, height: 1500 },
});

test.beforeAll(({}, { config }) => {
  const { baseURL } = config.projects[0].use;
  let mockedNetwork = fs.readFileSync('./test/e2e/loggedin/logger_base.har', { encoding: 'utf-8' });
  const expiryDate = new Date(new Date().getTime() + 1000 * 60 * 60 * 24 * 7).toISOString();

  if (baseURL) {
    // Replace base URL with correct URL
    const replacedMockedNetworkData = mockedNetwork.replace(/{{baseURL}}/g, baseURL || '');

    // Replace expiry dates with future date
    const replacedMockedNetworkDataJson = JSON.parse(replacedMockedNetworkData);
    replacedMockedNetworkDataJson.log.entries = replacedMockedNetworkDataJson.log.entries.map((entry) => {
      const text = entry?.response?.content?.text || '';
      if (text.includes('expiry')) {
        let jsonResponse = JSON.parse(text);
        if (jsonResponse && jsonResponse.resources?.[0]?.expiry) {
          jsonResponse.resources[0].expiry = expiryDate;
          entry.response.content.text = JSON.stringify(jsonResponse);
        }
      }
      return entry;
    });

    fs.writeFileSync('./test/e2e/loggedin/logger_test.har', JSON.stringify(replacedMockedNetworkDataJson), {
      encoding: 'utf-8',
    });
  }
});

test('testing logger component', async ({ page }, { config }) => {
  const { baseURL } = config.projects[0].use;

  await page.routeFromHAR('./test/e2e/loggedin/logger_test.har', { url: '**/api/**' });
  await page.goto(baseURL + '/ui/welcome');

  await page.getByRole('link', { name: 'deployments' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployments');
  await page.waitForTimeout(200);
  await page.getByRole('link', { name: '19b97ba0' }).click();
  await expect(page).toHaveURL(baseURL + '/ui/deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18');
  await page.getByRole('link', { name: 'Logs' }).click();
  await expect(page).toHaveURL(
    baseURL + '/ui/deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18?deployments-detail-tab=logs'
  );
  await page.getByText('Select components').click();
  await page.getByRole('option', { name: 'web' }).click();

  // set up creating resource log
  await page.route('api/deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18/create-log', (route) => {
    route.fulfill({
      status: 200,
      body: '{\n  "status" : 201,\n  "message" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743 created",\n  "resource-id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n}',
    });
  });

  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch', (route) => {
    route.fulfill({
      status: 200,
      body: '{\n  "status" : 202,\n  "message" : "starting resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743 with async job/c6bab054-f92a-4747-b6fe-07681e6c1fd0",\n  "resource-id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "location" : "job/c6bab054-f92a-4747-b6fe-07681e6c1fd0"\n}',
    });
  });

  // set up first resource-log request
  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', (route) => {
    route.fulfill({
      status: 200,
      body: resourceLogResponses[0].content.text,
    });
  });
  await page.locator('a[aria-label=play]').click();

  await waitForLogger(page);

  // First log line should be hidden, because auto scrolled to bottom
  expect(
    page.getByText(
      /2023-02-23T11:57:43\.986552573Z\s+127\.0\.0\.1\s+-\s+-\s+\[23\/Feb\/2023:11:57:43\s+\+0000\]\s+"GET\s+\//
    )
  ).toBeHidden();

  // disable auto scroll
  await page.getByText('Go Live').click();

  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', (route) => {
    route.fulfill({
      status: 200,
      body: resourceLogResponses[1].content.text,
    });
  });

  await waitForLogger(page);

  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', (route) => {
    route.fulfill({
      status: 200,
      body: resourceLogResponses[2].content.text,
    });
  });
  await waitForLogger(page);

  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', (route) => {
    route.fulfill({
      status: 200,
      body: resourceLogResponses[3].content.text,
    });
  });
  await waitForLogger(page);

  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', (route) => {
    route.fulfill({
      status: 200,
      body: resourceLogResponses[4].content.text,
    });
  });

  await waitForLogger(page);

  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', (route) => {
    route.fulfill({
      status: 200,
      body: resourceLogResponses[5].content.text,
    });
  });

  await waitForLogger(page);

  await page.route('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', (route) => {
    route.fulfill({
      status: 200,
      body: resourceLogResponses[6].content.text,
    });
  });
  await waitForLogger(page);

  expect(
    page.getByText(
      /2023-02-23T11:59:18\.674765152Z\s+127\.0\.0\.1\s+-\s+-\s+\[23\/Feb\/2023:11:59:18\s+\+0000\]\s+"GET\s+\//
    )
  ).toBeVisible();

  await page.locator('a[aria-label=pause]').click();
  expect(page.locator('.cm-line')).toHaveCount(48);
  await page.getByText('Clear').click();
  await page.waitForTimeout(200);
  expect(page.locator('.cm-line')).toHaveCount(1);
  expect(page.locator('.cm-line')).toHaveText('');
});

function waitForLogger(page: Page) {
  return page.waitForResponse('api/resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743', { timeout: 10200 });
}

const resourceLogResponses = [
  {
    status: 200,
    statusText: '',
    httpVersion: 'HTTP/2.0',
    cookies: [],
    headers: [
      { name: 'alt-svc', value: 'h3=":443"; ma=2592000' },
      { name: 'content-length', value: '1259' },
      { name: 'content-type', value: 'application/json' },
      { name: 'date', value: 'Thu, 23 Feb 2023 11:59:03 GMT' },
      { name: 'server', value: 'Caddy' },
      { name: 'server', value: 'Aleph/0.4.4' },
    ],
    content: {
      size: -1,
      mimeType: 'application/json',
      text: '{\n  "since" : "2023-02-23T11:57:42.132Z",\n  "parent" : "deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18",\n  "updated" : "2023-02-23T11:59:03.593Z",\n  "created" : "2023-02-23T11:59:03.593Z",\n  "components" : [ "web" ],\n  "created-by" : "internal",\n  "id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "resource-type" : "resource-log",\n  "acl" : {\n    "edit-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "owners" : [ "group/nuvla-admin" ],\n    "view-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "delete" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "manage" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ]\n  },\n  "operations" : [ {\n    "rel" : "edit",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "delete",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "fetch",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch"\n  } ]\n}',
    },
    headersSize: -1,
    bodySize: -1,
    redirectURL: '',
  },
  {
    status: 200,
    statusText: '',
    httpVersion: 'HTTP/2.0',
    cookies: [],
    headers: [
      { name: 'alt-svc', value: 'h3=":443"; ma=2592000' },
      { name: 'content-length', value: '6790' },
      { name: 'content-type', value: 'application/json' },
      { name: 'date', value: 'Thu, 23 Feb 2023 11:59:08 GMT' },
      { name: 'server', value: 'Caddy' },
      { name: 'server', value: 'Aleph/0.4.4' },
    ],
    content: {
      size: -1,
      mimeType: 'application/json',
      text: '{\n  "since" : "2023-02-23T11:57:42.132Z",\n  "parent" : "deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18",\n  "updated" : "2023-02-23T11:59:04.854Z",\n  "created" : "2023-02-23T11:59:03.593Z",\n  "updated-by" : "group/nuvla-admin",\n  "components" : [ "web" ],\n  "created-by" : "internal",\n  "id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "resource-type" : "resource-log",\n  "acl" : {\n    "edit-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "owners" : [ "group/nuvla-admin" ],\n    "view-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "delete" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "manage" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ]\n  },\n  "operations" : [ {\n    "rel" : "edit",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "delete",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "fetch",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch"\n  } ],\n  "last-timestamp" : "2023-02-23T11:59:04.561Z",\n  "log" : {\n    "web" : [ "2023-02-23T11:57:43.986552573Z 127.0.0.1 - - [23/Feb/2023:11:57:43 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:45.998709990Z 127.0.0.1 - - [23/Feb/2023:11:57:45 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:48.013247841Z 127.0.0.1 - - [23/Feb/2023:11:57:48 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:50.026360678Z 127.0.0.1 - - [23/Feb/2023:11:57:50 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:52.038302257Z 127.0.0.1 - - [23/Feb/2023:11:57:52 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:54.050600781Z 127.0.0.1 - - [23/Feb/2023:11:57:54 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:56.064705017Z 127.0.0.1 - - [23/Feb/2023:11:57:56 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:58.080362957Z 127.0.0.1 - - [23/Feb/2023:11:57:58 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:00.093424022Z 127.0.0.1 - - [23/Feb/2023:11:58:00 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:02.105682980Z 127.0.0.1 - - [23/Feb/2023:11:58:02 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:04.117312327Z 127.0.0.1 - - [23/Feb/2023:11:58:04 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:06.131256855Z 127.0.0.1 - - [23/Feb/2023:11:58:06 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:08.144958742Z 127.0.0.1 - - [23/Feb/2023:11:58:08 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:10.156256308Z 127.0.0.1 - - [23/Feb/2023:11:58:10 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:12.168228694Z 127.0.0.1 - - [23/Feb/2023:11:58:12 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:14.179859812Z 127.0.0.1 - - [23/Feb/2023:11:58:14 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:16.191339230Z 127.0.0.1 - - [23/Feb/2023:11:58:16 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:18.205750186Z 127.0.0.1 - - [23/Feb/2023:11:58:18 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:20.219411043Z 127.0.0.1 - - [23/Feb/2023:11:58:20 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:22.233109545Z 127.0.0.1 - - [23/Feb/2023:11:58:22 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:24.245170215Z 127.0.0.1 - - [23/Feb/2023:11:58:24 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:26.259238748Z 127.0.0.1 - - [23/Feb/2023:11:58:26 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:28.270778483Z 127.0.0.1 - - [23/Feb/2023:11:58:28 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:30.283080990Z 127.0.0.1 - - [23/Feb/2023:11:58:30 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:32.309811568Z 127.0.0.1 - - [23/Feb/2023:11:58:32 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:34.323607081Z 127.0.0.1 - - [23/Feb/2023:11:58:34 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:36.335791272Z 127.0.0.1 - - [23/Feb/2023:11:58:36 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:38.348475423Z 127.0.0.1 - - [23/Feb/2023:11:58:38 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:40.360453099Z 127.0.0.1 - - [23/Feb/2023:11:58:40 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:42.372239864Z 127.0.0.1 - - [23/Feb/2023:11:58:42 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:44.384458739Z 127.0.0.1 - - [23/Feb/2023:11:58:44 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:46.397888303Z 127.0.0.1 - - [23/Feb/2023:11:58:46 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:48.410108343Z 127.0.0.1 - - [23/Feb/2023:11:58:48 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:50.421866054Z 127.0.0.1 - - [23/Feb/2023:11:58:50 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:52.433946546Z 127.0.0.1 - - [23/Feb/2023:11:58:52 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:54.448029526Z 127.0.0.1 - - [23/Feb/2023:11:58:54 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:56.461933041Z 127.0.0.1 - - [23/Feb/2023:11:58:56 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:58.473633505Z 127.0.0.1 - - [23/Feb/2023:11:58:58 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:00.488070010Z 127.0.0.1 - - [23/Feb/2023:11:59:00 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:02.510762110Z 127.0.0.1 - - [23/Feb/2023:11:59:02 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:04.561760587Z 127.0.0.1 - - [23/Feb/2023:11:59:04 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"" ]\n  }\n}',
    },
    headersSize: -1,
    bodySize: -1,
    redirectURL: '',
  },
  {
    status: 200,
    statusText: '',
    httpVersion: 'HTTP/2.0',
    cookies: [],
    headers: [
      { name: 'alt-svc', value: 'h3=":443"; ma=2592000' },
      { name: 'content-length', value: '6790' },
      { name: 'content-type', value: 'application/json' },
      { name: 'date', value: 'Thu, 23 Feb 2023 11:59:08 GMT' },
      { name: 'server', value: 'Caddy' },
      { name: 'server', value: 'Aleph/0.4.4' },
    ],
    content: {
      size: -1,
      mimeType: 'application/json',
      text: '{\n  "since" : "2023-02-23T11:57:42.132Z",\n  "parent" : "deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18",\n  "updated" : "2023-02-23T11:59:04.854Z",\n  "created" : "2023-02-23T11:59:03.593Z",\n  "updated-by" : "group/nuvla-admin",\n  "components" : [ "web" ],\n  "created-by" : "internal",\n  "id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "resource-type" : "resource-log",\n  "acl" : {\n    "edit-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "owners" : [ "group/nuvla-admin" ],\n    "view-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "delete" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "manage" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ]\n  },\n  "operations" : [ {\n    "rel" : "edit",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "delete",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "fetch",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch"\n  } ],\n  "last-timestamp" : "2023-02-23T11:59:04.561Z",\n  "log" : {\n    "web" : [ "2023-02-23T11:57:43.986552573Z 127.0.0.1 - - [23/Feb/2023:11:57:43 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:45.998709990Z 127.0.0.1 - - [23/Feb/2023:11:57:45 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:48.013247841Z 127.0.0.1 - - [23/Feb/2023:11:57:48 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:50.026360678Z 127.0.0.1 - - [23/Feb/2023:11:57:50 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:52.038302257Z 127.0.0.1 - - [23/Feb/2023:11:57:52 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:54.050600781Z 127.0.0.1 - - [23/Feb/2023:11:57:54 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:56.064705017Z 127.0.0.1 - - [23/Feb/2023:11:57:56 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:57:58.080362957Z 127.0.0.1 - - [23/Feb/2023:11:57:58 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:00.093424022Z 127.0.0.1 - - [23/Feb/2023:11:58:00 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:02.105682980Z 127.0.0.1 - - [23/Feb/2023:11:58:02 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:04.117312327Z 127.0.0.1 - - [23/Feb/2023:11:58:04 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:06.131256855Z 127.0.0.1 - - [23/Feb/2023:11:58:06 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:08.144958742Z 127.0.0.1 - - [23/Feb/2023:11:58:08 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:10.156256308Z 127.0.0.1 - - [23/Feb/2023:11:58:10 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:12.168228694Z 127.0.0.1 - - [23/Feb/2023:11:58:12 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:14.179859812Z 127.0.0.1 - - [23/Feb/2023:11:58:14 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:16.191339230Z 127.0.0.1 - - [23/Feb/2023:11:58:16 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:18.205750186Z 127.0.0.1 - - [23/Feb/2023:11:58:18 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:20.219411043Z 127.0.0.1 - - [23/Feb/2023:11:58:20 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:22.233109545Z 127.0.0.1 - - [23/Feb/2023:11:58:22 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:24.245170215Z 127.0.0.1 - - [23/Feb/2023:11:58:24 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:26.259238748Z 127.0.0.1 - - [23/Feb/2023:11:58:26 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:28.270778483Z 127.0.0.1 - - [23/Feb/2023:11:58:28 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:30.283080990Z 127.0.0.1 - - [23/Feb/2023:11:58:30 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:32.309811568Z 127.0.0.1 - - [23/Feb/2023:11:58:32 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:34.323607081Z 127.0.0.1 - - [23/Feb/2023:11:58:34 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:36.335791272Z 127.0.0.1 - - [23/Feb/2023:11:58:36 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:38.348475423Z 127.0.0.1 - - [23/Feb/2023:11:58:38 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:40.360453099Z 127.0.0.1 - - [23/Feb/2023:11:58:40 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:42.372239864Z 127.0.0.1 - - [23/Feb/2023:11:58:42 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:44.384458739Z 127.0.0.1 - - [23/Feb/2023:11:58:44 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:46.397888303Z 127.0.0.1 - - [23/Feb/2023:11:58:46 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:48.410108343Z 127.0.0.1 - - [23/Feb/2023:11:58:48 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:50.421866054Z 127.0.0.1 - - [23/Feb/2023:11:58:50 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:52.433946546Z 127.0.0.1 - - [23/Feb/2023:11:58:52 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:54.448029526Z 127.0.0.1 - - [23/Feb/2023:11:58:54 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:56.461933041Z 127.0.0.1 - - [23/Feb/2023:11:58:56 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:58:58.473633505Z 127.0.0.1 - - [23/Feb/2023:11:58:58 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:00.488070010Z 127.0.0.1 - - [23/Feb/2023:11:59:00 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:02.510762110Z 127.0.0.1 - - [23/Feb/2023:11:59:02 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:04.561760587Z 127.0.0.1 - - [23/Feb/2023:11:59:04 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"" ]\n  }\n}',
    },
    headersSize: -1,
    bodySize: -1,
    redirectURL: '',
  },
  {
    status: 200,
    statusText: '',
    httpVersion: 'HTTP/2.0',
    cookies: [],
    headers: [
      { name: 'alt-svc', value: 'h3=":443"; ma=2592000' },
      { name: 'content-length', value: '1774' },
      { name: 'content-type', value: 'application/json' },
      { name: 'date', value: 'Thu, 23 Feb 2023 11:59:18 GMT' },
      { name: 'server', value: 'Caddy' },
      { name: 'server', value: 'Aleph/0.4.4' },
    ],
    content: {
      size: -1,
      mimeType: 'application/json',
      text: '{\n  "since" : "2023-02-23T11:57:42.132Z",\n  "parent" : "deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18",\n  "updated" : "2023-02-23T11:59:09.489Z",\n  "created" : "2023-02-23T11:59:03.593Z",\n  "updated-by" : "group/nuvla-admin",\n  "components" : [ "web" ],\n  "created-by" : "internal",\n  "id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "resource-type" : "resource-log",\n  "acl" : {\n    "edit-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "owners" : [ "group/nuvla-admin" ],\n    "view-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "delete" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "manage" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ]\n  },\n  "operations" : [ {\n    "rel" : "edit",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "delete",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "fetch",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch"\n  } ],\n  "last-timestamp" : "2023-02-23T11:59:08.587Z",\n  "log" : {\n    "web" : [ "2023-02-23T11:59:04.561760587Z 127.0.0.1 - - [23/Feb/2023:11:59:04 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:06.576342166Z 127.0.0.1 - - [23/Feb/2023:11:59:06 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:08.587542313Z 127.0.0.1 - - [23/Feb/2023:11:59:08 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"" ]\n  }\n}',
    },
    headersSize: -1,
    bodySize: -1,
    redirectURL: '',
  },
  {
    status: 200,
    statusText: '',
    httpVersion: 'HTTP/2.0',
    cookies: [],
    headers: [
      { name: 'alt-svc', value: 'h3=":443"; ma=2592000' },
      { name: 'content-length', value: '1774' },
      { name: 'content-type', value: 'application/json' },
      { name: 'date', value: 'Thu, 23 Feb 2023 11:59:18 GMT' },
      { name: 'server', value: 'Caddy' },
      { name: 'server', value: 'Aleph/0.4.4' },
    ],
    content: {
      size: -1,
      mimeType: 'application/json',
      text: '{\n  "since" : "2023-02-23T11:57:42.132Z",\n  "parent" : "deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18",\n  "updated" : "2023-02-23T11:59:09.489Z",\n  "created" : "2023-02-23T11:59:03.593Z",\n  "updated-by" : "group/nuvla-admin",\n  "components" : [ "web" ],\n  "created-by" : "internal",\n  "id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "resource-type" : "resource-log",\n  "acl" : {\n    "edit-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "owners" : [ "group/nuvla-admin" ],\n    "view-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "delete" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "manage" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ]\n  },\n  "operations" : [ {\n    "rel" : "edit",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "delete",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "fetch",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch"\n  } ],\n  "last-timestamp" : "2023-02-23T11:59:08.587Z",\n  "log" : {\n    "web" : [ "2023-02-23T11:59:04.561760587Z 127.0.0.1 - - [23/Feb/2023:11:59:04 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:06.576342166Z 127.0.0.1 - - [23/Feb/2023:11:59:06 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:08.587542313Z 127.0.0.1 - - [23/Feb/2023:11:59:08 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"" ]\n  }\n}',
    },
    headersSize: -1,
    bodySize: -1,
    redirectURL: '',
  },
  {
    status: 200,
    statusText: '',
    httpVersion: 'HTTP/2.0',
    cookies: [],
    headers: [
      { name: 'alt-svc', value: 'h3=":443"; ma=2592000' },
      { name: 'content-length', value: '2170' },
      { name: 'content-type', value: 'application/json' },
      { name: 'date', value: 'Thu, 23 Feb 2023 11:59:28 GMT' },
      { name: 'server', value: 'Caddy' },
      { name: 'server', value: 'Aleph/0.4.4' },
    ],
    content: {
      size: -1,
      mimeType: 'application/json',
      text: '{\n  "since" : "2023-02-23T11:57:42.132Z",\n  "parent" : "deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18",\n  "updated" : "2023-02-23T11:59:19.418Z",\n  "created" : "2023-02-23T11:59:03.593Z",\n  "updated-by" : "group/nuvla-admin",\n  "components" : [ "web" ],\n  "created-by" : "internal",\n  "id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "resource-type" : "resource-log",\n  "acl" : {\n    "edit-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "owners" : [ "group/nuvla-admin" ],\n    "view-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "delete" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "manage" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ]\n  },\n  "operations" : [ {\n    "rel" : "edit",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "delete",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "fetch",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch"\n  } ],\n  "last-timestamp" : "2023-02-23T11:59:18.674Z",\n  "log" : {\n    "web" : [ "2023-02-23T11:59:08.587542313Z 127.0.0.1 - - [23/Feb/2023:11:59:08 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:10.599179499Z 127.0.0.1 - - [23/Feb/2023:11:59:10 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:12.615261338Z 127.0.0.1 - - [23/Feb/2023:11:59:12 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:14.627901324Z 127.0.0.1 - - [23/Feb/2023:11:59:14 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:16.657537056Z 127.0.0.1 - - [23/Feb/2023:11:59:16 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:18.674765152Z 127.0.0.1 - - [23/Feb/2023:11:59:18 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"" ]\n  }\n}',
    },
    headersSize: -1,
    bodySize: -1,
    redirectURL: '',
  },
  {
    status: 200,
    statusText: '',
    httpVersion: 'HTTP/2.0',
    cookies: [],
    headers: [
      { name: 'alt-svc', value: 'h3=":443"; ma=2592000' },
      { name: 'content-length', value: '2170' },
      { name: 'content-type', value: 'application/json' },
      { name: 'date', value: 'Thu, 23 Feb 2023 11:59:28 GMT' },
      { name: 'server', value: 'Caddy' },
      { name: 'server', value: 'Aleph/0.4.4' },
    ],
    content: {
      size: -1,
      mimeType: 'application/json',
      text: '{\n  "since" : "2023-02-23T11:57:42.132Z",\n  "parent" : "deployment/19b97ba0-d7c1-4532-aa84-59db8d76bc18",\n  "updated" : "2023-02-23T11:59:19.418Z",\n  "created" : "2023-02-23T11:59:03.593Z",\n  "updated-by" : "group/nuvla-admin",\n  "components" : [ "web" ],\n  "created-by" : "internal",\n  "id" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743",\n  "resource-type" : "resource-log",\n  "acl" : {\n    "edit-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "owners" : [ "group/nuvla-admin" ],\n    "view-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "delete" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-acl" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "view-data" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "manage" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ],\n    "edit-meta" : [ "session/73c98d5d-5fee-44ea-84a5-7ea734f94af4" ]\n  },\n  "operations" : [ {\n    "rel" : "edit",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "delete",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743"\n  }, {\n    "rel" : "fetch",\n    "href" : "resource-log/756d6e6b-fc1e-48c2-a0fc-c47537201743/fetch"\n  } ],\n  "last-timestamp" : "2023-02-23T11:59:18.674Z",\n  "log" : {\n    "web" : [ "2023-02-23T11:59:08.587542313Z 127.0.0.1 - - [23/Feb/2023:11:59:08 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:10.599179499Z 127.0.0.1 - - [23/Feb/2023:11:59:10 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:12.615261338Z 127.0.0.1 - - [23/Feb/2023:11:59:12 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:14.627901324Z 127.0.0.1 - - [23/Feb/2023:11:59:14 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:16.657537056Z 127.0.0.1 - - [23/Feb/2023:11:59:16 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"", "2023-02-23T11:59:18.674765152Z 127.0.0.1 - - [23/Feb/2023:11:59:18 +0000] \\"GET / HTTP/1.1\\" 200 615 \\"-\\" \\"curl/7.74.0\\" \\"-\\"" ]\n  }\n}',
    },
    headersSize: -1,
    bodySize: -1,
    redirectURL: '',
  },
];
