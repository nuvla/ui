import type { PlaywrightTestConfig } from '@playwright/test';
import { devices } from '@playwright/test';

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
require('dotenv').config({ path: './.env.e2e' });

let baseURL = process.env.UI_BASE_URL || '';
/**
 * Blow up if now valid URL was provided
 */
if (isValidHttpUrl(baseURL)) {
  console.log('Test running on', baseURL);
} else {
  console.error('Not a valid url:', baseURL);
  throw new Error('not a valid baseURL: ' + baseURL);
}

/**
 * See https://playwright.dev/docs/test-configuration.
 */
const config: PlaywrightTestConfig = {
  testDir: './test/e2e',
  grepInvert: [/apps/],
  /* Maximum time one test can run for. */
  timeout: 60 * 1000,
  expect: {
    /**
     * Maximum time expect() should wait for the condition to be met.
     * For example in `await expect(locator).toHaveText();`
     */
    timeout: 5000,
  },
  /* Run tests in files in parallel */
  fullyParallel: false,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 3 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : 1,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: process.env.CI ? [['github'], ['html'], ['list']] : 'html',
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    screenshot: 'only-on-failure',
    video: process.env.CI ? 'retain-on-failure' : 'on',
    actionTimeout: 0,
    timezoneId: 'Europe/Zurich',
    locale: 'de-CH',
    baseURL,
    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: 'on',
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'global setup',
      testMatch: 'global-setup.ts',
      teardown: 'global teardown',

    },
    {
      name: 'global teardown',
      testMatch: 'global-teardown.ts',
      use: {
        storageState: 'storageState.json'
      }
    },
    {
      name: 'logged-in-tests',
      dependencies: ['global setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: 'storageState.json',

        //         launchOptions: {
        //               slowMo: 500,
        //             },
      }
    },
  ],
};

export default config;

function isValidHttpUrl(s: string) {
  let url: URL;
  try {
    url = new URL(s);
  } catch (_) {
    return false;
  }
  return url.protocol === 'http:' || url.protocol === 'https:';
}
