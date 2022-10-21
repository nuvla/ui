export const mockedInfrasctureServiceData = {
  "subtype='registry'": {
    count: 1,
    acl: {
      query: ['group/nuvla-user'],
      add: ['group/nuvla-user'],
    },
    'resource-type': 'infrastructure-service-collection',
    id: 'infrastructure-service',
    resources: [
      {
        name: 'Docker Hub Registry',
        'resource-type': 'infrastructure-service',
        id: 'infrastructure-service/ada09d3c-e03d-4aab-a89a-d57ddf0c6fa4',
      },
    ],
    operations: [
      {
        rel: 'add',
        href: 'infrastructure-service',
      },
    ],
  },
  "subtype='swarm'": {
    count: 1,
    acl: {
      query: ['group/nuvla-user'],
      add: ['group/nuvla-user'],
    },
    'resource-type': 'infrastructure-service-collection',
    id: 'infrastructure-service',
    resources: [
      {
        capabilities: ['NUVLA_JOB_PULL'],
        subtype: 'swarm',
        name: 'Swarm Edging',
        description: 'Swarm cluster on Edging',
        'resource-type': 'infrastructure-service',
        acl: {
          'view-meta': [
            'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
            'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
            'user/90783050-e223-43db-a112-107ef509c6d3',
          ],
          'view-acl': [
            'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
            'user/90783050-e223-43db-a112-107ef509c6d3',
          ],
          'view-data': [
            'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
            'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
            'user/90783050-e223-43db-a112-107ef509c6d3',
          ],
          owners: ['group/nuvla-admin'],
        },
        id: 'infrastructure-service/3bc97314-a4c4-49e8-8515-19b70b011a11',
      },
    ],
    operations: [
      {
        rel: 'add',
        href: 'infrastructure-service',
      },
    ],
  },
  "(subtype='registry') and ((id='infrastructure-service/ada09d3c-e03d-4aab-a89a-d57ddf0c6fa4'))": {
    count: 1,
    acl: {
      query: ['group/nuvla-user'],
      add: ['group/nuvla-user'],
    },
    'resource-type': 'infrastructure-service-collection',
    id: 'infrastructure-service',
    resources: [
      {
        name: 'Docker Hub Registry',
        description: 'Private and public repositories on Docker Hub (hub.docker.com)',
        'resource-type': 'infrastructure-service',
        id: 'infrastructure-service/ada09d3c-e03d-4aab-a89a-d57ddf0c6fa4',
      },
    ],
    operations: [
      {
        rel: 'add',
        href: 'infrastructure-service',
      },
    ],
  },
};

export const credentials = {
  GET: {
    description: 'NuvlaBox credential linked to Edging',
    key: '-----BEGIN RSA PRIVATE KEY-----\n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\n-----END RSA PRIVATE KEY-----\n',
    parent: 'infrastructure-service/3bc97314-a4c4-49e8-8515-19b70b011a11',
    method: 'infrastructure-service-swarm',
    updated: '2022-10-21T07:08:11.810Z',
    name: 'Edging',
    ca: '-----BEGIN CERTIFICATE-----\nMIIFYzCCA0ugAwIBAgIUT4Gk1oUlJ8AkNJqtZjFK7zZEfDYwDQYJKoZIhvcNAQEL\nBQAwQTELMAkGA1UEBhMCQ0gxDzANBgNVBAcMBkdlbmV2YTEOMAwGA1UECgwFU2l4\nU3ExETAPBgNVBAMMCG51dmxhYm94MB4XDTIyMTAxNzEwMDE1N1oXDTIzMDExNTEw\nMDE1N1owQTELMAkGA1UEBhMCQ0gxDzANBgNVBAcMBkdlbmV2YTEOMAwGA1UECgwF\nU2l4U3ExETAPBgNVBAMMCG51dmxhYm94MIICIjANBgkqhkiG9w0BAQEFAAOCAg8A\nMIICCgKCAgEA1dX6PdKgA+tzDbF546g48W6uIo/OPihImZ3lL1ze0zlojoFZxiRh\nLcmoSXUYpg80Gy9fJ3rSyKHz56YqxrxHs/GXX7boHqaPGxbb1/xXUrxYx8GppB2+\nrfixPvoThRGZQQ46gXs+XFndjdhZqrePfYQcvS7swt0Y+III9xG8HUQDFTx7fLdP\nBojNiwfZZx9bePOoSJ2/skuvTgCvcStAlI8hFBanOgv9Lt1zEF5+Px406S45N2EZ\nRgYJKRiLEQa52/N9y8eDJzIT7adHy6NW8rsYoge5kVdssh9QtDI9XF1VHqDqg/LI\nHVr9yxNJYZzHiHy219Sgw/hGlFeEROCf94ZQdbBAXUxA2tzZDwiukBkk6swo3V2u\nDxDGzOMiojO10ee0WZv27ZVSHR4+v/xfbIilzr9h1K3g5QdZfC651aGVrFdisf22\nsUoN0gqWDleqqo3lmX5c35pcao9ZDGw7/g/TRmEHV82E8g++WmR2+BUEimuehLa7\nRfZrPGemDrG3XtJDOiLlm5knySuqeAFrJMkzsMskEJ9P9NYoIRDu4Bdv+IuCNOJQ\nsQe55eQb6sgsI8H9ZhU2+16mE6V3hf5wVUi1B7/l2on7kMgyTZY4er8udzVX6pTa\ncRuiFty0A2ULbtTGnKfjTmHcxR/fFZWyNQl0TWpkg0DYZH4P02FUFwMCAwEAAaNT\nMFEwHQYDVR0OBBYEFGu4HyxmbBfDozruKOzcvrEdN+zRMB8GA1UdIwQYMBaAFGu4\nHyxmbBfDozruKOzcvrEdN+zRMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQEL\nBQADggIBAHRFbNlwzNAQojURNOOMR7/OzmaROaXCPcxNqLnN/lbKZMaRHTbsAh4L\n92S09dUoBGxWvA88t/HuRwsMUcqNIE7CXa3yhdx50z06v4gJJgBs9WmIDgwFOvcG\niYZIOaDtXYrF3hmYhzKlRRoHZcKF2isMyVUMWkdlE7xxBFBX6pwDuS5bADVX1XnU\nbav4sI82WjqtbyCQxgERrtwjNYbh9GzDSbjKZteBq0x8EmaKV4/E1aoO3LCgLVQC\nlLwe6SK7Thr1oCJjfW+LkgmXEWr9WnFR4ZM20jVnKQ9gLSfySuoN6tdVGxWWog5+\nu7/Y5YOZT+zJLjJUDKTWRLjUGVT41bJGuL4+knkXdQoxIzwATgc5N36IBPX1/nSe\ndFDS1iELpJUpljwzJFYShGX4gsDeUn147/qvB5qR0VFktHX1rUNcu9t9HyDsqwHZ\nLR2nRoHsSw2N0DFdiWOwIyI3DOG6lZXKuOHvwHHkohTIgeQ7iT0rkYb/lO76EjfA\nKGUjOiq3zkLiAgUJBUh+IVEZeT9bIsjtwSTgmhq3NRhV7uDTE58zt2sa6pLVLkBG\n+rbQR5ObE1CJDPZchWk/RW032Fim0gpD+wSVlonV1wPByKm+RoqulkInsvTg5iBv\nMY5CTc45Sd1JiEE4V9MkWDzxpl3jBVBK8zidUOGkHllsTU4F1ZB7\n-----END CERTIFICATE-----\n',
    cert: '-----BEGIN CERTIFICATE-----\nMIIE9zCCAt+gAwIBAgIUD2DvIxYif3EfU9dFF9SScfZJpMgwDQYJKoZIhvcNAQEL\nBQAwQTELMAkGA1UEBhMCQ0gxDzANBgNVBAcMBkdlbmV2YTEOMAwGA1UECgwFU2l4\nU3ExETAPBgNVBAMMCG51dmxhYm94MB4XDTIyMTAxNzEwMDE1OFoXDTIzMDExNTEw\nMDE1OFowETEPMA0GA1UEAwwGY2xpZW50MIICIjANBgkqhkiG9w0BAQEFAAOCAg8A\nMIICCgKCAgEA2OXeTqEVllBJ8tjH7LRsysRwcNenFyKRAvxvHBGx2LRW8qE7plpZ\n5dFDnbDje/jEw61Qu2UtE8iSVQGBWM/7KUGdaqtw/qiL25xU5qaJpS1jo9WYNgXG\noPTgG34mFa8r9heTx9SwJsC9JLY4TQFnRXM8v8Cuk52zxdtQz/esxT6VnmLdgr/d\n8DMP/u18GWg0h83y00uey2hE28IxSXfGFeSkZljR5tY54fP+9r3S9L3po1S/x3HL\nWC6TOB51Td0CTDY8yeuBxKyIIiFCTSgenLFOmMTA2z35W/9AisMOdcPaQw5BMDzQ\nJQe98ZHO6QxXlxZsEoEnJi9Dmj5MN5ML3YyNTmGnXb6UBeWeznkOEqOCALpeMOtJ\nQaRhLJAYW1eIoIiP9XEOu0Ht//T4/doVdFFeN45CUx8wdXcZBqpW4uzSHgGgpq/b\nmDTQp1tGlkxLwa7RPMgWNUT515cK9RdCw5WrhJk/0uigQB6mF14ga2yoNd8IJeQp\nA7lJuGl6qg8IV/KtuHpsDElRHxk4ITQiYspD9PpPUDxjvo3iYQPTKiouNJ7BGI3N\nuvrsmQuWOlbU+2hJSJyoCrwKidPaL4WiWYs7UJiCvWjJzt+mIJ29eVrDLhQWbdsk\nZoIts7rmIhJIXEbAi67cUIdWEdhteeGyTYuBD56f5naUR3vsI5WNfPUCAwEAAaMX\nMBUwEwYDVR0lBAwwCgYIKwYBBQUHAwIwDQYJKoZIhvcNAQELBQADggIBAEiailPA\nxx2Qt2a+GTKggLg9v6jn8dBqWODPkKQ2Z2iktarKD9YoQNViReIJDhZdy5am6zkW\nLZRcN+8QW4kuSLqGl6SL/QtK6WOzeJDMpwghLMuovuFfhk1mXknqqH3aj7HMSxs8\n4luipZ9vY2j2pnbSZee6BVhk/zV9P2ZN9T/5XszsbcjL307vsaWR4PYzRi6bip0+\nLFmpQD73UUsK5KhSeVHDEnuXlC+lX63L+krFbor5dvVeUQzMrRME4nq6B1w2ZOQQ\np2myw4gh1nJzAaRPu2IhBz52wNX8EkFQk7YPqTov/ry1zr9gp1nWEgq6upJAOpH1\nWORx2FeR3S70Y9hnlHfJwFGjWH4DLl2rB/+JyF8kIdGZpZtJri+8T7GJeYceC0fm\nQxsshIzuPriBP2jPNHk1Trnj7XjpgOgeVFckGbRzrgzrLU+9CoJdYQfwLRLlAEY0\nE9kF0+kTcMo3ZROm+EftMRm9t4oEUMLX/J6+uTWBvXLmejGm0IIWwtVECgUBkJp4\nMdkN0ocZ+jX8ZLAM9htO3Qqf9nHGb5ldGl494xiiG7+g5Eyvwhy04BRZhuff5/ot\ntvhMY7tbnMVpqzvJEDnYA9AyapKNRX++DIizEVsAK5qBRKKlqmSj5HjwIWSO1oT3\nDM0Gp6PktLuvlOs8AtZHnTURgZA+HTTKMWXA\n-----END CERTIFICATE-----\n',
    'last-check': '2022-10-21T06:50:18.801Z',
    created: '2022-10-17T10:02:00.046Z',
    'updated-by': 'internal',
    'created-by': 'internal',
    status: 'UNKNOWN',
    id: 'credential/5c860aa4-e840-4cc1-9248-e857f33ac474',
    'resource-type': 'credential',
    acl: {
      'view-acl': [
        'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
        'user/90783050-e223-43db-a112-107ef509c6d3',
      ],
      'view-meta': [
        'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
        'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
        'user/90783050-e223-43db-a112-107ef509c6d3',
      ],
      'view-data': [
        'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
        'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
        'user/90783050-e223-43db-a112-107ef509c6d3',
      ],
      manage: [
        'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
        'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
        'user/90783050-e223-43db-a112-107ef509c6d3',
      ],
      owners: ['group/nuvla-admin'],
    },
    operations: [
      {
        rel: 'check',
        href: 'credential/5c860aa4-e840-4cc1-9248-e857f33ac474/check',
      },
    ],
    subtype: 'infrastructure-service-swarm',
  },
  PUT: {
    "(parent='infrastructure-service/3bc97314-a4c4-49e8-8515-19b70b011a11') and (subtype='infrastructure-service-swarm')":
      {
        count: 1,
        acl: {
          query: ['group/nuvla-user', 'group/nuvla-nuvlabox'],
          add: ['group/nuvla-user', 'group/nuvla-nuvlabox'],
        },
        'resource-type': 'credential-collection',
        id: 'credential',
        resources: [
          {
            description: 'NuvlaBox credential linked to Edging',
            name: 'Edging',
            'last-check': '2022-10-21T06:50:18.801Z',
            created: '2022-10-17T10:02:00.046Z',
            status: 'UNKNOWN',
            id: 'credential/5c860aa4-e840-4cc1-9248-e857f33ac474',
            'resource-type': 'credential',
            acl: {
              'view-meta': [
                'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
                'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
                'user/90783050-e223-43db-a112-107ef509c6d3',
              ],
              'view-acl': [
                'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
                'user/90783050-e223-43db-a112-107ef509c6d3',
              ],
              'view-data': [
                'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
                'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
                'user/90783050-e223-43db-a112-107ef509c6d3',
              ],
              owners: ['group/nuvla-admin'],
              manage: [
                'infrastructure-service/eb8e09c2-8387-4f6d-86a4-ff5ddf3d07d7',
                'nuvlabox/88de338a-df9d-43df-a03b-ec646efda988',
                'user/90783050-e223-43db-a112-107ef509c6d3',
              ],
            },
            operations: [
              {
                rel: 'check',
                href: 'credential/5c860aa4-e840-4cc1-9248-e857f33ac474/check',
              },
            ],
            subtype: 'infrastructure-service-swarm',
          },
        ],
        operations: [
          {
            rel: 'add',
            href: 'credential',
          },
        ],
      },
  },
};
