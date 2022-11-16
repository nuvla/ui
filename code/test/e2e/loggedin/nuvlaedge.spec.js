import { test, expect } from '@playwright/test';

test('NuvlaEdge creation and deletion', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.waitForResponse((resp) => resp.url().includes('get-subscription'));
  await page.getByRole('link', { name: 'Edges' }).click();

  const edgesPageRegex = /\/ui\/edges$/;

  await expect(page).toHaveURL(edgesPageRegex);

  await page.getByText('Add').click();

  await page.getByText('bluetooth').click();

  await page.getByText('gpu').click();

  await page.getByText('modbus').click();

  await page.getByText('network').click();

  await page.getByText('usb').first().click();

  await page.getByText('Compose file bundle').click();

  await page.getByText('Enable host-level management').click();

  const newEdgeName = `e2e Tesing: Edge creation and deletion in ${project.name} ${new Date().toISOString()}`;

  await page.locator('input[type="input"]').click();
  await page.locator('input[type="input"]').fill(newEdgeName);

  await page.getByRole('button', { name: 'create' }).click();

  await page.locator('span:has-text("Host-level management") i').nth(1).click();

  await page
    .locator('span:has-text("Enabled and ready to be used. Please copy this cronjob and add it to your system") i')
    .click();

  await page.locator('.close').click();

  await page.getByRole('link', { name: new RegExp(`${newEdgeName}`) }).click();

  await page.getByText('Disable host level management').click({ timeout: 5000 });
  await page.getByRole('button', { name: 'Disable Host Level Management' }).click();
  await page.getByText('success executing operation disable-host-level-management').click();

  await page.getByText(/^delete$/i).click();
  await page.getByRole('button', { name: 'delete' }).click();
  await expect(page).toHaveURL(edgesPageRegex);

  // This is a workaround to test clipboard content if cronjob is copied
  // see: https://github.com/microsoft/playwright/issues/8114
  const modifier = process.env.MODIFIER_KEY || 'Control';
  await page.setContent(`<div contenteditable>123</div>`);
  await page.focus('div');
  await page.keyboard.press(`${modifier}+KeyV`);
  await page.keyboard.press(`${modifier}+KeyV`);
  const cronjob = await page.evaluate(() => document.querySelector('div').textContent);
  expect(cronjob.startsWith('* 0 * * * ')).toBeTruthy();
  for (const envVar of ['NUVLABOX_API_KEY', 'NUVLABOX_API_SECRET', 'NUVLA_ENDPOINT']) {
    const testRegex = new RegExp(` (${envVar})=`);
    const [_, matchedEnvVar] = cronjob.match(testRegex) || [];

    expect(matchedEnvVar).toBe(envVar);
  }
});

/**
 * Used these tests for local verification of update modal changes #959, but they need more work to be run in CI.
 */
test.skip('NuvlaEdge update to version with security module from unofficial release', async ({ page, context }, {
  project,
  config,
}) => {
  const releaseVersion = '2.1.0';
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.route('api/nuvlabox-status', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBosStatus(releaseVersion)) });
  });
  await page.route('api/nuvlabox-status/**', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBoxStatuForNB(releaseVersion)) });
  });
  await page.pause();
  // 1. Go to Edges page
  // 2. Go to Click dream big edge
  // 3. Update Edge
  // 4. No checkboxes available
  // 5. When changing nuvla edge version to 2.4.3 security module should be checked
});

test.skip('NuvlaEdge update to version with security module from official release', async ({ page, context }, {
  project,
  config,
}) => {
  const releaseVersion = '2.1.1';
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.route('api/nuvlabox-status', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBosStatus(releaseVersion)) });
  });
  await page.route('api/nuvlabox-status/**', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBoxStatuForNB(releaseVersion)) });
  });
  await page.pause();
  // 1. Go to Edges page
  // 2. Go to Click dream big edge
  // 3. Update Edge
  // 4. Checkboxes should be available, only GPU and Network checked
  // 5. When changing nuvla edge version to 2.4.3 security module should be checked
});

test.skip('NuvlaEdge update from version with security module but not installed', async ({ page, context }, {
  project,
  config,
}) => {
  const releaseVersion = '2.3.1';
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.route('api/nuvlabox-status', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBosStatus(releaseVersion)) });
  });
  await page.route('api/nuvlabox-status/**', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBoxStatuForNB(releaseVersion)) });
  });
  await page.pause();
  // 1. Go to Edges page
  // 2. Go to Click dream big edge
  // 3. Update Edge
  // 4. When changing nuvla edge version to 2.4.3 security module should NOT be unchecked
});

test.skip('NuvlaEdge upgrade from version with security module and installed', async ({ page, context }, {
  project,
  config,
}) => {
  const releaseVersion = '2.3.1';
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.route('api/nuvlabox-status', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBosStatus('2.3.0')) });
  });
  await page.route('api/nuvlabox-status/**', async (route) => {
    route.fulfill({ status: 200, body: JSON.stringify(getNuvlaBoxStatuForNB('2.3.1', 'docker-compose.security.yml')) });
  });
  await page.pause();
  // 1. Go to Edges page
  // 2. Go to Click dream big edge
  // 3. Update Edge
  // 4. When changing nuvla edge version to 2.4.3 security module should be checked
});

const getNuvlaBoxStatuForNB = (version = '2.1.1', additionalModule = '') => ({
  'cluster-id': 'znqevgdskqrncpuyirbn09oy9',
  orchestrator: 'swarm',
  'current-time': '2022-11-14T07:48:33Z',
  ip: '',
  'cluster-managers': ['zr232zw6b2dzt8whbzup27j5p'],
  parent: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
  architecture: 'aarch64',
  'container-plugins': [],
  updated: '2022-11-14T08:00:01.498Z',
  'last-boot': '2022-11-13T05:53:29Z',
  'status-notes': [],
  'nuvlabox-engine-version': version,
  'docker-server-version': '20.10.20',
  created: '2022-10-31T17:09:45.878Z',
  hostname: 'docker-desktop',
  'cluster-join-address': '192.168.65.3:2377',
  'installation-parameters': {
    'project-name': 'nuvlabox',
    environment: [
      'SKIP_MINIMUM_REQUIREMENTS=False',
      'PYTHON_GET_PIP_URL=https://github.com/pypa/get-pip/raw/936e08ce004d0b2fae8952c50f7ccce1bc578ce5/public/get-pip.py',
      'NUVLA_ENDPOINT_INSECURE=False',
      'PYTHON_GET_PIP_SHA256=8890955d56a8262348470a76dc432825f61a84a54e2985a86cd520f656a6e220',
      'DOCKER_CHANNEL=stable',
      'NUVLA_ENDPOINT=nuvla.io',
      'PYTHONWARNINGS=ignore:Unverified HTTPS request',
      'VPN_INTERFACE_NAME=vpn',
      'NUVLABOX_IMMUTABLE_SSH_PUB_KEY=',
      'PYTHON_PIP_VERSION=21.1.2',
      'IMAGE_NAME=job-lite',
      'NUVLABOX_DATA_GATEWAY_IMAGE=eclipse-mosquitto:1.6.12',
      'PYTHON_VERSION=3.8.10',
      'PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      'DOCKER_VERSION=20.10.7',
      'NUVLABOX_UUID=nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
      'LANG=C.UTF-8',
      'HOST=00a56cffbfee',
      'HOST_HOME=/Users/mariotrost',
      'NUVLABOX_ENGINE_VERSION=2.1.1',
      'GPG_KEY=E3FF2839C048B25C084DEBE9B26995E310250568',
    ],
    'working-dir': '/Users',
    'config-files': ['docker-compose.gpu.yml', additionalModule, 'docker-compose.network.yml', 'docker-compose.yml'],
  },
  'swarm-node-cert-expiry-date': '2023-01-29T17:09:00Z',
  jobs: ['job/013c9e7d-0e0f-4b65-bd7b-0752d0fee082'],
  online: false,
  'host-user-home': '/Users/mariotrost',
  'updated-by': 'group/nuvla-admin',
  components: [
    'vpn-client',
    'nuvlabox_peripheral-manager-gpu_1',
    'nuvlabox_peripheral-manager-network_1',
    'nuvlabox_agent_1',
    'data-gateway.1.4zc71cqwrzv90mbl42ho7x1kw',
    'nuvlabox_system-manager_1',
    'nuvlabox-job-engine-lite',
    'compute-api',
    'nuvlabox-on-stop',
    '095f658f-bc46-45b7-9951-65c7357755b2_trigger-blackbox.1.4jc87n63hep7cu7afnlbgj91v',
    '095f658f-bc46-45b7-9951-65c7357755b2_object-detector.1.lqvbzhx0vj2n4ty1xeaax63gq',
    '430921ec-c7b1-47a2-9661-7088e96d659e_rabbitmq.1.zhgdv7atye4y6ojsljd25rrmh',
  ],
  'created-by': 'internal',
  status: 'OPERATIONAL',
  'node-id': 'zr232zw6b2dzt8whbzup27j5p',
  id: 'nuvlabox-status/b8876fd2-74e3-4d39-96b7-d5e4b36df81c',
  'operating-system': 'Docker Desktop 5.15.49-linuxkit',
  'resource-type': 'nuvlabox-status',
  'cluster-nodes': ['zr232zw6b2dzt8whbzup27j5p'],
  acl: {
    'view-acl': [
      'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
      'user/90783050-e223-43db-a112-107ef509c6d3',
    ],
    'view-meta': [
      'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
      'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
      'user/90783050-e223-43db-a112-107ef509c6d3',
    ],
    'view-data': [
      'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
      'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
      'user/90783050-e223-43db-a112-107ef509c6d3',
    ],
    'edit-data': ['nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d'],
    'edit-meta': ['nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d'],
    owners: ['group/nuvla-admin'],
  },
  'next-heartbeat': '2022-11-14T07:59:35.807Z',
  'cluster-node-role': 'manager',
  version: 2,
  resources: {
    'net-stats': [
      {
        interface: 'eth0',
        'bytes-transmitted': 13729209090,
        'bytes-received': 8979894608,
      },
      {
        interface: 'docker0',
        'bytes-transmitted': 167580111,
        'bytes-received': 86375175,
      },
      {
        interface: 'br-a4c42ff18288',
        'bytes-transmitted': 1637926194,
        'bytes-received': 1198871362,
      },
      {
        interface: 'docker_gwbridge',
        'bytes-transmitted': 3375122,
        'bytes-received': 1993320,
      },
      {
        interface: 'services1',
        'bytes-transmitted': 3923861,
        'bytes-received': 4198457,
      },
      {
        interface: 'vpn',
        'bytes-transmitted': 50588,
        'bytes-received': 0,
      },
    ],
    'container-stats': [
      {
        'mem-usage-limit': '91.6MiB / 7850.6MiB',
        'cpu-percent': '62.60',
        'net-in-out': '0.0MB / 0.0MB',
        'restart-count': 0,
        'container-status': 'running',
        name: 'vpn-client',
        id: '31ec4e5c1bdba42abc22dcec623a7950038a28ae7478c34b6aa91c1552814345',
        'mem-percent': '1.17',
        'blk-in-out': '0.0MB / 1.5MB',
      },
      {
        'mem-usage-limit': '0.0MiB / 0.0MiB',
        'cpu-percent': 'nan',
        'net-in-out': '0.0MB / 0.0MB',
        'restart-count': 284,
        'container-status': 'restarting',
        name: 'nuvlabox_peripheral-manager-gpu_1',
        id: 'ff18a71183ed9a95a33e7bbec9e664a3dbaf068568b83ebe7e8e99802e9bc102',
        'mem-percent': '0.00',
        'blk-in-out': '0.0MB / 0.0MB',
      },
      {
        'mem-usage-limit': '19.0MiB / 7850.6MiB',
        'cpu-percent': '0.00',
        'net-in-out': '0.0MB / 0.0MB',
        'restart-count': 0,
        'container-status': 'running',
        name: 'nuvlabox_peripheral-manager-network_1',
        id: '5932a2b57228fb9e6974c2aa220f13fae5858237b8d241ba2827409a822e5e66',
        'mem-percent': '0.24',
        'blk-in-out': '0.0MB / 2.7MB',
      },
      {
        'mem-usage-limit': '60.6MiB / 7850.6MiB',
        'cpu-percent': '0.01',
        'net-in-out': '11.5MB / 5.2MB',
        'restart-count': 7,
        'container-status': 'running',
        name: 'nuvlabox_agent_1',
        id: '718c1d99b4d7530ca2f01a6fef789c793466b2656932073becf14b3a86514f59',
        'mem-percent': '0.77',
        'blk-in-out': '0.0MB / 15.4MB',
      },
      {
        'mem-usage-limit': '0.8MiB / 7850.6MiB',
        'cpu-percent': '0.07',
        'net-in-out': '5.2MB / 1.5MB',
        'restart-count': 0,
        'container-status': 'running',
        name: 'data-gateway.1.4zc71cqwrzv90mbl42ho7x1kw',
        id: '730d7df12fcd5e5668b4f77a75554ed17437ab356db770607c46e68ac52eccc7',
        'mem-percent': '0.01',
        'blk-in-out': '0.0MB / 0.0MB',
      },
      {
        'mem-usage-limit': '57.6MiB / 7850.6MiB',
        'cpu-percent': '5.22',
        'net-in-out': '3.3MB / 3.0MB',
        'restart-count': 0,
        'container-status': 'running',
        name: 'nuvlabox_system-manager_1',
        id: 'f1ffe5ae722fa754da78fb0fbe933c0c49397de293bbb902ddaf3761e651d9e1',
        'mem-percent': '0.73',
        'blk-in-out': '0.0MB / 22.3MB',
      },
      {
        'mem-usage-limit': '15.9MiB / 7850.6MiB',
        'cpu-percent': '0.00',
        'net-in-out': '0.2MB / 0.0MB',
        'restart-count': 0,
        'container-status': 'paused',
        name: 'nuvlabox-job-engine-lite',
        id: 'aa120fb724f8f9d7d8e4df03d24c0f4c96ce8e69b33410ff9a042949a55cb63a',
        'mem-percent': '0.20',
        'blk-in-out': '0.0MB / 0.0MB',
      },
      {
        'mem-usage-limit': '6.0MiB / 7850.6MiB',
        'cpu-percent': '0.00',
        'net-in-out': '5.3MB / 15.3MB',
        'restart-count': 0,
        'container-status': 'running',
        name: 'compute-api',
        id: '610d24856008f84bc130e2c2772867114543baf7544963edaf7307ee7cb3414f',
        'mem-percent': '0.08',
        'blk-in-out': '0.0MB / 0.0MB',
      },
      {
        'mem-usage-limit': '14.3MiB / 7850.6MiB',
        'cpu-percent': '0.00',
        'net-in-out': '0.2MB / 0.0MB',
        'restart-count': 0,
        'container-status': 'paused',
        name: 'nuvlabox-on-stop',
        id: '194255628ba5e2b73b8621942c8ec70a3a2b831b1059782813cc1bf3615d4984',
        'mem-percent': '0.18',
        'blk-in-out': '0.0MB / 0.0MB',
      },
      {
        'mem-usage-limit': '9.2MiB / 7850.6MiB',
        'cpu-percent': '0.01',
        'net-in-out': '0.7MB / 0.1MB',
        'restart-count': 0,
        'container-status': 'running',
        name: '095f658f-bc46-45b7-9951-65c7357755b2_trigger-blackbox.1.4jc87n63hep7cu7afnlbgj91v',
        id: '4ba85c4d193b9ffafe89afdf2a7bc3e3ede1bd781c6bc8a8b32bd4a0b39a8bb6',
        'mem-percent': '0.12',
        'blk-in-out': '0.0MB / 0.0MB',
      },
      {
        'mem-usage-limit': '34.5MiB / 7850.6MiB',
        'cpu-percent': '0.01',
        'net-in-out': '0.6MB / 0.0MB',
        'restart-count': 0,
        'container-status': 'running',
        name: '095f658f-bc46-45b7-9951-65c7357755b2_object-detector.1.lqvbzhx0vj2n4ty1xeaax63gq',
        id: '691e129942eeb11f4efe54c5a14c863f55e3a8b5503d03578457568e4ee00876',
        'mem-percent': '0.44',
        'blk-in-out': '0.0MB / 0.0MB',
      },
      {
        'mem-usage-limit': '90.3MiB / 7850.6MiB',
        'cpu-percent': '0.33',
        'net-in-out': '0.7MB / 0.1MB',
        'restart-count': 0,
        'container-status': 'running',
        name: '430921ec-c7b1-47a2-9661-7088e96d659e_rabbitmq.1.zhgdv7atye4y6ojsljd25rrmh',
        id: '8898229795a6ca04cf2ff9b0ef70e69b60ac0643466829203400bcee81d99b18',
        'mem-percent': '1.15',
        'blk-in-out': '0.0MB / 5.8MB',
      },
    ],
    cpu: {
      load: 12.06591796875,
      'system-calls': 0,
      capacity: 4,
      interrupts: 501519510,
      topic: 'cpu',
      'software-interrupts': 224059094,
      'raw-sample':
        '{"capacity": 4, "load": 12.06591796875, "load_1": 18.98876953125, "load_5": 16.40283203125, "context_switches": 866802984, "interrupts": 501519510, "software_interrupts": 224059094, "system_calls": 0}',
      'load-5': 16.40283203125,
      'context-switches': 866802984,
      'load-1': 18.98876953125,
    },
    ram: {
      topic: 'ram',
      'raw-sample': '{"capacity": 7851, "used": 1599}',
      capacity: 7851,
      used: 1599,
    },
    disks: [
      {
        topic: 'disks',
        'raw-sample': '{"device": "vda1", "capacity": 60, "used": 7}',
        device: 'vda1',
        capacity: 60,
        used: 7,
      },
    ],
  },
  'inferred-location': [8, 47],
});

const getNuvlaBosStatus = (version = '2.1.1') => ({
  count: 1,
  acl: {
    add: ['group/nuvla-admin'],
    query: ['group/nuvla-user'],
  },
  'resource-type': 'nuvlabox-status-collection',
  id: 'nuvlabox-status',
  resources: [
    {
      parent: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
      'nuvlabox-engine-version': version,
      'next-heartbeat': '2022-11-14T07:59:35.807Z',
      online: false,
      'resource-type': 'nuvlabox-status',
      acl: {
        'edit-data': ['nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d'],
        'view-meta': [
          'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
          'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
          'user/90783050-e223-43db-a112-107ef509c6d3',
        ],
        'view-acl': [
          'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
          'user/90783050-e223-43db-a112-107ef509c6d3',
        ],
        'view-data': [
          'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
          'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
          'user/90783050-e223-43db-a112-107ef509c6d3',
        ],
        'edit-meta': ['nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d'],
        owners: ['group/nuvla-admin'],
      },
      id: 'nuvlabox-status/b8876fd2-74e3-4d39-96b7-d5e4b36df81c',
    },
  ],
});
