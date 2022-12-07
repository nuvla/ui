import { test, expect } from '@playwright/test';

test('NuvlaEdge creation and deletion', async ({ page, context }, { project, config }) => {
  const { baseURL } = config.projects[0].use;
  await page.goto(baseURL + '/ui/welcome');
  await page.waitForResponse((resp) => resp.url().includes('get-subscription'));

  await page.route(
    (url) => {
      return url.pathname.endsWith('nuvlabox') || url.pathname.includes('api/nuvlabox/');
    },
    (route) => {
      const url = new URL(route.request().url());

      const file = url.pathname.split('/').slice(-1)[0];
      if (file === 'nuvlabox') {
        route.fulfill({
          status: 200,
          body: JSON.stringify(mocknuvlaboxes()),
        });
      } else {
        route.fulfill({
          status: 200,
          body: JSON.stringify(mocknuvlaboxes().resources.find((edge) => edge.id === 'nuvlabox/' + file)),
        });
      }
    }
  );
  await page.route('api/nuvlabox-status/**', (route) => {
    route.fulfill({
      status: 200,
      body: JSON.stringify(nuvlaboxStatusMock),
    });
  });

  await page.getByRole('link', { name: 'Edges' }).click();

  await page.getByText('e2e-1', { exact: true }).click();

  await expect(page.locator('td:text("public") + td')).toHaveText('123.123.123.123');
  await expect(page.locator('td:text("swarm") + td')).toHaveText('234.234.234.234');
  await expect(page.locator('td:text("local") + td')).toHaveText('345.345.345.345');

  await page.getByText('8 interfaces, 9 IPs').click();

  await expect(page.locator('td:text("eth0") + td')).toHaveText('111.111.111.111, 222.222.222.222');
  await expect(page.locator('td:text("docker0") + td')).toHaveText('333.333.333.333');
  await expect(page.locator('td:text("docker_gwbridge") + td')).toHaveText('444.444.444.444');
  await expect(page.locator('td:text("br-63bf8b19c07c") + td')).toHaveText('555.555.555.555');
  await expect(page.locator('td:text("br-9f554fddf09f") + td')).toHaveText('666.666.666.666');
  await expect(page.locator('td:text("br-00f62a45526d") + td')).toHaveText('777.777.777.777');
  await expect(page.locator('td:text("br-282adf02575a") + td')).toHaveText('888.888.888.888');
  await expect(page.locator('td:text("br-d037ba84c3c8") + td')).toHaveText('999.999.999.999');
});

const mocknuvlaboxes = () => ({
  count: 4,
  acl: {
    query: ['group/nuvla-user'],
    add: ['group/nuvla-user'],
  },
  'resource-type': 'nuvlabox-collection',
  id: 'nuvlabox',
  resources: [
    { ...oneBox, name: 'e2e-1', id: 'nuvlabox/1' },
    { ...oneBox, name: 'e2e-2', id: 'nuvlabox/2' },
    { ...oneBox, name: 'e2e-3', id: 'nuvlabox/3' },
    { ...oneBox, name: 'e2e-4', id: 'nuvlabox/3' },
  ],
  operations: [
    {
      rel: 'add',
      href: 'nuvlabox',
    },
  ],
});

const oneBox = {
  capabilities: ['NUVLA_JOB_PULL'],
  tags: ['nuvlabox=True'],
  'refresh-interval': 30,
  updated: '2022-11-16T18:54:06.387Z',
  name: 'em2e 1',
  'credential-api-key': '1234567890',
  created: '2022-10-31T17:02:32.513Z',
  state: 'COMMISSIONED',
  'vpn-server-id': 'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
  online: false,
  'infrastructure-service-group': 'infrastructure-service-group/138fa215-6ad9-4c8c-9802-cc1320c5625b',
  'created-by': 'user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2',
  id: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
  'resource-type': 'nuvlabox',
  acl: {
    'edit-data': ['user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2'],
    owners: ['group/nuvla-admin'],
    'view-acl': [
      'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
      'user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2',
    ],
    delete: ['user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2'],
    'view-meta': [
      'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
      'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
      'user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2',
    ],
    'edit-acl': ['user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2'],
    'view-data': [
      'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
      'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
      'user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2',
    ],
    manage: ['nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d', 'user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2'],
    'edit-meta': ['user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2'],
  },
  operations: [
    {
      rel: 'edit',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d',
    },
    {
      rel: 'commission',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/commission',
    },
    {
      rel: 'decommission',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/decommission',
    },
    {
      rel: 'add-ssh-key',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/add-ssh-key',
    },
    {
      rel: 'revoke-ssh-key',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/revoke-ssh-key',
    },
    {
      rel: 'update-nuvlabox',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/update-nuvlabox',
    },
    {
      rel: 'cluster-nuvlabox',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/cluster-nuvlabox',
    },
    {
      rel: 'reboot',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/reboot',
    },
    {
      rel: 'assemble-playbooks',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/assemble-playbooks',
    },
    {
      rel: 'enable-emergency-playbooks',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/enable-emergency-playbooks',
    },
    {
      rel: 'enable-host-level-management',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/enable-host-level-management',
    },
    {
      rel: 'create-log',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/create-log',
    },
    {
      rel: 'generate-new-api-key',
      href: 'nuvlabox/4f52d8bb-2925-46b9-b3f0-cb2fb0beb87d/generate-new-api-key',
    },
  ],
  'nuvlabox-status': 'nuvlabox-status/b8876fd2-74e3-4d39-96b7-d5e4b36df81c',
  version: 2,
  'inferred-location': [8.7241, 47.5056],
  owner: 'user/2d48d9d8-a08e-4d29-86fc-891b013e8ff2',
};

const nuvlaboxStatusMock = {
  'current-time': '2022-11-16T16:26:31Z',
  ip: '222.222.222.222',
  parent: 'nuvlabox/mocked-id',
  architecture: 'x86_64',
  'container-plugins': [],
  updated: '2022-11-16T16:27:00.449Z',
  'last-boot': '2022-11-09T15:11:26Z',
  'status-notes': ['WARNING: No swap limit support'],
  'nuvlabox-engine-version': 'default-gw-report',
  created: '2022-11-11T20:14:59.396Z',
  hostname: 'nuvla-mocked-deployment',
  'installation-parameters': {
    'project-name': 'nuvlabox',
    environment: [
      'HOST_HOME=/root',
      'DOCKER_CHANNEL=stable',
      'HOST=nuvlabox',
      'NUVLABOX_ENGINE_VERSION=default-gw-report',
      'PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin',
      'NUVLABOX_IMMUTABLE_SSH_PUB_KEY=',
      'PYTHON_VERSION=3.8.10',
      'PYTHON_GET_PIP_SHA256=8890955d56a8262348470a76dc432825f61a84a54e2985a86cd520f656a6e220',
      'PYTHONWARNINGS=ignore:Unverified HTTPS request',
      'NUVLA_ENDPOINT=https://nuvla.io',
      'SKIP_MINIMUM_REQUIREMENTS=False',
      'NUVLABOX_DATA_GATEWAY_IMAGE=eclipse-mosquitto:1.6.12',
      'NUVLA_ENDPOINT_INSECURE=False',
      'PYTHON_GET_PIP_URL=https://github.com/pypa/get-pip/raw/936e08ce004d0b2fae8952c50f7ccce1bc578ce5/public/get-pip.py',
      'IMAGE_NAME=job-lite',
      'LANG=C.UTF-8',
      'PYTHON_PIP_VERSION=21.1.2',
      'DOCKER_VERSION=20.10.7',
      'NUVLABOX_UUID=nuvlabox/e7d0419e-ae07-4d33-88cd-4b2c2a32e888',
      'VPN_INTERFACE_NAME=vpn',
    ],
    'working-dir': '/root/nuvlabox',
    'config-files': ['/root/nuvlabox/docker-compose.yml'],
  },
  jobs: [],
  online: true,
  'host-user-home': '/root',
  'updated-by': 'nuvlabox/e7d0419e-ae07-4d33-88cd-4b2c2a32e888',
  components: [
    'nuvlabox-agent-1',
    'data-gateway.1.swbe2ljfgrfpnlm2lliql0dxt',
    'nuvlabox-system-manager-1',
    'nuvlabox-on-stop',
    'nuvlabox-job-engine-lite',
    'compute-api',
  ],
  'created-by': 'internal',
  status: 'OPERATIONAL',
  id: 'nuvlabox-status/3a9970f0-2ac6-4eff-a3d3-463344c83809',
  'operating-system': 'Ubuntu 18.04.6 LTS 4.15.0-169-generic',
  'resource-type': 'nuvlabox-status',
  acl: {
    'view-acl': ['user/e1867e90-9514-4313-95e3-90da74356904'],
    'view-meta': ['nuvlabox/e7d0419e-ae07-4d33-88cd-4b2c2a32e888', 'user/e1867e90-9514-4313-95e3-90da74356904'],
    'view-data': ['nuvlabox/e7d0419e-ae07-4d33-88cd-4b2c2a32e888', 'user/e1867e90-9514-4313-95e3-90da74356904'],
    'edit-data': ['nuvlabox/e7d0419e-ae07-4d33-88cd-4b2c2a32e888'],
    'edit-meta': ['nuvlabox/e7d0419e-ae07-4d33-88cd-4b2c2a32e888'],
    owners: ['group/nuvla-admin'],
  },
  operations: [
    {
      rel: 'edit',
      href: 'nuvlabox-status/3a9970f0-2ac6-4eff-a3d3-463344c83809',
    },
    {
      rel: 'delete',
      href: 'nuvlabox-status/3a9970f0-2ac6-4eff-a3d3-463344c83809',
    },
  ],
  'next-heartbeat': '2022-11-16T16:28:10.450Z',
  network: {
    'default-gw': 'eth0',
    ips: {
      public: '123.123.123.123',
      swarm: '234.234.234.234',
      vpn: '',
      local: '345.345.345.345',
    },
    interfaces: [
      {
        interface: 'eth0',
        ips: [
          {
            address: '111.111.111.111',
          },
          {
            address: '222.222.222.222',
          },
        ],
      },
      {
        interface: 'docker0',
        ips: [
          {
            address: '333.333.333.333',
          },
        ],
      },
      {
        interface: 'docker_gwbridge',
        ips: [
          {
            address: '444.444.444.444',
          },
        ],
      },
      {
        interface: 'br-63bf8b19c07c',
        ips: [
          {
            address: '555.555.555.555',
          },
        ],
      },
      {
        interface: 'br-9f554fddf09f',
        ips: [
          {
            address: '666.666.666.666',
          },
        ],
      },
      {
        interface: 'br-00f62a45526d',
        ips: [
          {
            address: '777.777.777.777',
          },
        ],
      },
      {
        interface: 'br-282adf02575a',
        ips: [
          {
            address: '888.888.888.888',
          },
        ],
      },
      {
        interface: 'br-d037ba84c3c8',
        ips: [
          {
            address: '999.999.999.999',
          },
        ],
      },
    ],
  },
  version: 2,
  resources: {
    'net-stats': [
      {
        interface: 'eth0',
        'bytes-transmitted': 210557748,
        'bytes-received': 286844063,
      },
      {
        interface: 'docker0',
        'bytes-transmitted': 180,
        'bytes-received': 0,
      },
      {
        interface: 'docker_gwbridge',
        'bytes-transmitted': 120768470,
        'bytes-received': 572420268,
      },
      {
        interface: 'br-63bf8b19c07c',
        'bytes-transmitted': 0,
        'bytes-received': 0,
      },
      {
        interface: 'br-9f554fddf09f',
        'bytes-transmitted': 0,
        'bytes-received': 0,
      },
      {
        interface: 'br-00f62a45526d',
        'bytes-transmitted': 0,
        'bytes-received': 0,
      },
      {
        interface: 'br-282adf02575a',
        'bytes-transmitted': 0,
        'bytes-received': 0,
      },
      {
        interface: 'br-d037ba84c3c8',
        'bytes-transmitted': 232087135,
        'bytes-received': 45922579,
      },
    ],
    cpu: {
      load: 0.72607421875,
      'system-calls': 0,
      capacity: 2,
      interrupts: 2033488693,
      topic: 'cpu',
      'software-interrupts': 887739608,
      'raw-sample':
        '{"capacity": 2, "load": 0.72607421875, "load_1": 1.35498046875, "load_5": 0.947265625, "context_switches": 4919642424, "interrupts": 2033488693, "software_interrupts": 887739608, "system_calls": 0}',
      'load-5': 0.947265625,
      'context-switches': 4919642424,
      'load-1': 1.35498046875,
    },
    ram: {
      topic: 'ram',
      'raw-sample': '{"capacity": 3941, "used": 3340}',
      capacity: 3941,
      used: 3340,
    },
    disks: [
      {
        topic: 'disks',
        'raw-sample': '{"device": "vda1", "capacity": 50, "used": 20}',
        device: 'vda1',
        capacity: 50,
        used: 20,
      },
    ],
  },
  'inferred-location': [8.55, 47.3667],
};
