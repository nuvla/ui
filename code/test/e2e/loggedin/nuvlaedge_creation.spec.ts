import { test, expect } from '@playwright/test';

test('Creating an Edge with an older version',  async ({ page, context }, { project, config })  => {

   const { baseURL } = config.projects[0].use;
    await page.goto(baseURL + '/ui/welcome');
    await page.route('api/nuvlabox-release', async (route) => {
        route.fulfill({ status: 200, body: JSON.stringify(NuvlaEdgeRelease())});
      });
    await page.getByRole('link', { name: 'Edges' }).click();
    const edgeName = 'NE with older release';

  const edgesPageRegex = /\/ui\/edges/;

  await expect(page).toHaveURL(edgesPageRegex);

  await page.getByText('Add', { exact: true }).click();

  await page.locator('input[type="input"]').click();

  await page.locator('input[type="input"]').fill(edgeName);
  // We add the security module
  await page.getByText('security').click();

  await page.getByText('bluetooth').click();

  await page.getByText('gpu').click();

  await page.getByText('modbus').click();

  await page.locator('div[role="listbox"]:has-text("2.4.3") i').click();
  // We switch to an older version, for which the security module is not available
  await page.getByRole('option', { name: '1.11.0' }).click();

  await page.getByText('gpu').click();

  await page.locator('form > div:nth-child(2) > div').first().click();

  await page.getByText('Compose file bundle').click();

  await page.getByRole('button', { name: 'create' }).click();

  await page.getByRole('button', { name: 'close' }).click();
  // We verify that nothing has blown up, we get to see the edges table
  await expect(page.getByRole('link', { name: 'select row 0 ' + edgeName + ' 1.y.z' })).toBeVisible();

});

const NuvlaEdgeRelease = () => (
{
  "count" : 62,
  "acl" : {
    "query" : [ "group/nuvla-user" ]
  },
  "resource-type" : "nuvlabox-release-collection",
  "id" : "nuvlabox-release",
  "resources" : [ {
    "release-date" : "2022-10-27T12:23:14Z",
    "updated" : "2023-02-28T23:19:17.454Z",
    "release" : "2.4.3",
    "created" : "2022-10-27T12:36:05.600Z",
    "pre-release" : false,
    "compose-files" : [ {
      "name" : "docker-compose.bluetooth.yml",
      "scope" : "bluetooth",
      "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=bluetooth\"\n\nservices:\n  peripheral-manager-bluetooth:\n    <<: *common\n    image: nuvlabox/peripheral-manager-bluetooth:1.2.0\n    restart: on-failure\n    network_mode: host\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - agent\n"
    }, {
      "name" : "docker-compose.gpu.yml",
      "scope" : "gpu",
      "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=gpu\"\n\nservices:\n  peripheral-manager-gpu:\n    <<: *common\n    image: nuvlabox/peripheral-manager-gpu:1.1.0\n    restart: on-failure\n    volumes:\n      - /etc/:/etcfs/:ro\n      - /var/run/docker.sock:/var/run/docker.sock:ro\n      - /usr/lib/:/usr/lib/:ro\n      - /dev/:/dev/:ro\n    depends_on:\n      - agent\n"
    }, {
      "name" : "docker-compose.modbus.yml",
      "scope" : "modbus",
      "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=modbus\"\n\nservices:\n  peripheral-manager-modbus:\n    <<: *common\n    image: nuvlabox/peripheral-manager-modbus:1.3.0\n    restart: on-failure\n    depends_on:\n      - agent\n"
    }, {
      "name" : "docker-compose.network.yml",
      "scope" : "network",
      "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=network\"\n\nservices:\n  peripheral-manager-network:\n    <<: *common\n    image: nuvlabox/peripheral-manager-network:1.2.0\n    restart: on-failure\n    network_mode: host\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - agent\n"
    }, {
      "name" : "docker-compose.security.yml",
      "scope" : "security",
      "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=network\"\n\nservices:\n  security:\n    <<: *common\n    image: nuvlabox/security:2.1.0\n    restart: unless-stopped\n    network_mode: host\n    privileged: true\n    environment:\n      - EXTERNAL_CVE_VULNERABILITY_DB=${EXTERNAL_CVE_VULNERABILITY_DB:-\"https://github.com/nuvla/vuln-db/blob/main/databases/all.aggregated.csv.gz?raw=true\"}\n      - EXTERNAL_CVE_VULNERABILITY_DB_UPDATE_INTERVAL=${EXTERNAL_CVE_VULNERABILITY_DB_UPDATE_INTERVAL:-86400}\n      - SECURITY_SCAN_INTERVAL=${SECURITY_SCAN_INTERVAL:-1800}\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - agent\n"
    }, {
      "name" : "docker-compose.usb.yml",
      "scope" : "usb",
      "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=usb\"\n\nservices:\n  peripheral-manager-usb:\n    <<: *common\n    image: nuvlabox/peripheral-manager-usb:2.1.0\n    restart: always\n    network_mode: host\n    volumes:\n      - /dev:/dev:ro\n      - /run/udev/control:/run/udev/control:ro\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - agent"
    }, {
      "name" : "docker-compose.yml",
      "scope" : "",
      "file" : "version: \"3.7\"\n\nx-common: &common\n  stop_grace_period: 5s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n\nvolumes:\n  nuvlabox-db:\n    driver: local\n    labels:\n      - \"nuvlabox.volume=True\"\n      - \"nuvlabox.deployment=production\"\n\nservices:\n  system-manager:\n    <<: *common\n    image: nuvlabox/system-manager:2.4.0\n    restart: always\n    environment:\n      - SKIP_MINIMUM_REQUIREMENTS=${SKIP_MINIMUM_REQUIREMENTS:-False}\n      - NUVLABOX_DATA_GATEWAY_IMAGE=${NUVLABOX_DATA_GATEWAY_IMAGE:-eclipse-mosquitto:1.6.12}\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n      - nuvlabox-db:/srv/nuvlabox/shared\n    ports:\n      - 127.0.0.1:3636:3636\n    healthcheck:\n      test: [\"CMD\", \"curl\", \"-f\", \"http://localhost:3636\"]\n      interval: 30s\n      timeout: 10s\n      retries: 4\n      start_period: 10s\n    depends_on:\n      - on-stop\n\n  agent:\n    <<: *common\n    image: nuvlabox/agent:2.8.4\n    restart: always\n    oom_kill_disable: true\n    privileged: true\n    environment:\n      - NUVLABOX_API_KEY=${NUVLABOX_API_KEY}\n      - NUVLABOX_API_SECRET=${NUVLABOX_API_SECRET}\n      - NUVLABOX_UUID=${NUVLABOX_UUID}\n      - NUVLABOX_ENGINE_VERSION=2.4.3\n      - NUVLABOX_IMMUTABLE_SSH_PUB_KEY=${NUVLABOX_SSH_PUB_KEY}\n      - HOST_HOME=${HOME}\n      - VPN_INTERFACE_NAME=${VPN_INTERFACE_NAME:-vpn}\n      - NUVLA_ENDPOINT=${NUVLA_ENDPOINT:-nuvla.io}\n      - NUVLA_ENDPOINT_INSECURE=${NUVLA_ENDPOINT_INSECURE:-False}\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n      - nuvlabox-db:/srv/nuvlabox/shared\n      - /:/rootfs:ro\n    ports:\n      - 127.0.0.1:5080:80\n    healthcheck:\n      test: [\"CMD\", \"curl\", \"-f\", \"http://localhost/api/healthcheck\"]\n      interval: 30s\n      timeout: 10s\n      retries: 4\n      start_period: 10s\n    depends_on:\n      - compute-api\n\n  compute-api:\n    <<: *common\n    image: nuvlabox/compute-api:1.2.0\n    container_name: compute-api\n    restart: on-failure\n    oom_score_adj: -900\n    pid: \"host\"\n    environment:\n      - HOST=${HOSTNAME:-nuvlabox}\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n      - nuvlabox-db:/srv/nuvlabox/shared\n    ports:\n      - 5000:5000\n    healthcheck:\n      test: netstat -tulpn | grep LISTEN | grep 5000 | grep socat\n      interval: 20s\n      timeout: 10s\n      start_period: 30s\n\n  vpn-client:\n    <<: *common\n    image: nuvlabox/vpn-client:1.1.0\n    container_name: vpn-client\n    restart: always\n    oom_kill_disable: true\n    network_mode: host\n    privileged: true\n    devices:\n      - /dev/net/tun\n    environment:\n      - NUVLABOX_UUID=${NUVLABOX_UUID}\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - agent\n\n  job-engine-lite:\n    <<: *common\n    image: nuvla/job-lite:3.2.2\n    restart: always\n    container_name: nuvlabox-job-engine-lite\n    entrypoint: /app/pause.py\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n\n  on-stop:\n    <<: *common\n    image: nuvlabox/on-stop:1.1.0\n    restart: always\n    container_name: nuvlabox-on-stop\n    command: pause\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n"
    }, {
      "name" : "install.sh",
      "scope" : "",
      "file" : "#!/bin/sh\n\n# NuvlaBox Engine advanced installation script\n# This script is an alternative for the conventional one-command `docker-compose ... ` installation/halt/remove methods\n# It provides extra checks and guidance for making sure that:\n#  1. there are no existing NuvlaBox Engines already running\n#  2. handle existing installations before installing a new one\n#  3. checks installation requirements\n#  4. installs/updates/removes NuvlaBox Engine\n\ncompose_files=\"docker-compose.yml\"\nstrategies=\"UPDATE OVERWRITE\"\nstrategy=\"UPDATE\"\nactions=\"INSTALL REMOVE HALT\"\naction=\"INSTALL\"\nextra_env=\"\"\nenv_file=\"\"\n\nusage()\n{\n    echo \"NuvlaBox Engine advanced installation wrapper\"\n    echo \"\"\n    echo \"./install.sh\"\n    echo \"\"\n    echo \" -h --help\"\n    echo \" --environment=KEY1=value1,KEY2=value2\\t\\t(optional) Comma-separated environment keypair values\"\n    echo \" --env-path=PATH\\t\\t\\t\\t(optional) Path for env file\"\n    echo \" --compose-files=file1.yml,file2.yml\\t\\t(optional) Comma-separated list of compose files to deploy. Default: ${compose_files}\"\n    echo \" --installation-strategy=STRING\\t\\t\\t(optional) Strategy when action=INSTALL. Must be on of: ${strategies}. Default: ${strategy}\"\n    echo \"\\t\\t UPDATE - if NuvlaBox Engine is already running, replace outdated components and start stopped ones. Otherwise, install\"\n    echo \"\\t\\t OVERWRITE - if NuvlaBox Engine is already running, shut it down and re-install. Otherwise, install\"\n    echo \" --action=STRING\\t\\t\\t\\t(optional) What action to take. Must be on of: ${actions}. Default: ${action}\"\n    echo \"\\t\\t INSTALL - runs 'docker-compose up'\"\n    echo \"\\t\\t REMOVE - removes the NuvlaBox Engine and all associated data. Same as 'docker-compose down -v\"\n    echo \"\\t\\t HALT - shuts down the NuvlaBox Engine but keeps data, so it can be revived later. Same as 'docker-compose down\"\n    echo \"\"\n}\n\nwhile [ \"$1\" != \"\" ]; do\n    PARAM=`echo $1 | awk -F= '{print $1}'`\n    VALUE=`echo $1 | cut -d \"=\" -f 2-`\n    case $PARAM in\n        -h | --help)\n            usage\n            exit\n            ;;\n        --environment)\n            extra_env=$VALUE\n            ;;\n        --env-file)\n            env_file=$VALUE\n            ;;\n        --compose-files)\n            compose_files=$VALUE\n            ;;\n        --installation-strategy)\n            strategy=$VALUE\n            ;;\n        --action)\n            action=$VALUE\n            ;;\n        *)\n            echo \"ERROR: unknown parameter \\\"$PARAM\\\"\"\n            usage\n            exit 1\n            ;;\n    esac\n    shift\ndone\n\nwhich docker-compose >/dev/null\nif [ $? -ne 0 ]\nthen\n  echo \"ERR: docker-compose is not installed. Cannot continue\"\n  exit 1\nfi\n\nset -x\n\nif [ ! -z \"${extra_env}\" ]\nthen\n  echo \"Setting up environment ${extra_env}\"\n  export $(echo ${extra_env} | tr ',' ' ') &>/dev/null\nfi\n\ncommand_compose_files=\"\"\nfor file in $(echo ${compose_files} | tr ',' '\\n')\ndo\n  command_compose_files=\"${command_compose_files} -f ${file}\"\ndone\n\ncommand_env=\"\"\nif [ ! -z \"${env_file}\" ]\nthen\n  command_env=\"${command_env} --env-file ${env_file}\"\nfi\n\nif [ \"${action}\" = \"REMOVE\" ]\nthen\n  echo \"INFO: removing NuvlaBox installation completely\"\n  docker-compose -p nuvlabox ${command_compose_files} ${command_env} down -v\n  ([ ! -z \"${env_file}\" ] && rm \"${env_file}\")\nelif [ \"${action}\" = \"HALT\" ]\nthen\n  echo \"INFO: halting NuvlaBox. You can bring it back later by simply re-installing with the same parameters as before\"\n  docker-compose -p nuvlabox ${command_compose_files} ${command_env} down\nelif [ \"${action}\" = \"INSTALL\" ]\nthen\n  if [ \"${strategy}\" = \"UPDATE\" ]\n  then\n    existing_projects=$(docker-compose -p nuvlabox ${command_compose_files} ${command_env} ps -a -q)\n    if [ ! -z \"${existing_projects}\" ]\n    then\n      echo \"INFO: found an active NuvlaBox installation. Updating it\"\n    else\n      echo \"INFO: no active NuvlaBox installations found. Installing from scratch\"\n    fi\n    docker-compose -p nuvlabox ${command_compose_files} ${command_env} up -d\n  elif [ \"${strategy}\" = \"OVERWRITE\" ]\n  then\n    echo \"WARNING: about to delete any existing NuvlaBox installations...press Ctrl+c in the next 5 seconds to stop\"\n    sleep 5\n    docker-compose -p nuvlabox ${command_compose_files} ${command_env} down -v --remove-orphans\n    echo \"INFO: installing NuvlaBox Engine from scratch\"\n    docker-compose -p nuvlabox ${command_compose_files} ${command_env} up -d\n  else\n    echo \"WARNING: strategy ${strategy} not recognized. Use -h for help. Nothing to do\"\n  fi\nelse\n  echo \"WARNING: action ${action} not recognized. Use -h for help. Nothing to do\"\n  exit 0\nfi"
    } ],
    "updated-by" : "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
    "created-by" : "group/nuvla-admin",
    "id" : "nuvlabox-release/5dbe5376-955d-4bc4-a1b2-8043d4db8e82",
    "url" : "https://github.com/nuvlaedge/deployment/releases/tag/2.4.3",
    "resource-type" : "nuvlabox-release",
    "acl" : {
      "view-meta" : [ "group/nuvla-user" ],
      "view-data" : [ "group/nuvla-user" ],
      "owners" : [ "group/nuvla-admin" ]
    },
    "operations" : [ {
      "rel" : "edit",
      "href" : "nuvlabox-release/5dbe5376-955d-4bc4-a1b2-8043d4db8e82"
    }, {
      "rel" : "delete",
      "href" : "nuvlabox-release/5dbe5376-955d-4bc4-a1b2-8043d4db8e82"
    } ],
    "published" : false,
  }, {
    "release-date" : "2020-11-02T13:37:24Z",
    "updated" : "2023-02-28T23:23:42.109Z",
    "release" : "1.11.0",
    "created" : "2020-12-03T12:28:46.058Z",
    "pre-release" : false,
    "compose-files" : [ {
      "name" : "docker-compose.gpu.yml",
      "scope" : "gpu",
      "file" : "version: \"3.6\"\n\nx-common: &common\n  stop_grace_period: 4s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=gpu\"\n\nservices:\n  peripheral-manager-gpu:\n    <<: *common\n    image: nuvlabox/peripheral-manager-gpu:0.0.1\n    restart: on-failure\n    volumes:\n      - /etc:/etc:ro\n      - /var/run/docker.sock:/var/run/docker.sock:ro\n"
    }, {
      "name" : "docker-compose.modbus.yml",
      "scope" : "modbus",
      "file" : "version: \"3.6\"\n\nx-common: &common\n  stop_grace_period: 4s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=modbus\"\n\nservices:\n  peripheral-manager-modbus:\n    <<: *common\n    image: nuvlabox/peripheral-manager-modbus:1.0.0\n    restart: on-failure\n    environment:\n      - NUVLA_ENDPOINT=${NUVLA_ENDPOINT:-nuvla.io}\n      - NUVLA_ENDPOINT_INSECURE=${NUVLA_ENDPOINT_INSECURE:-False}\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared"
    }, {
      "name" : "docker-compose.usb.yml",
      "scope" : "usb",
      "file" : "version: \"3.6\"\n\nx-common: &common\n  stop_grace_period: 4s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n    - \"nuvlabox.peripheral.component=True\"\n    - \"nuvlabox.peripheral.type=usb\"\n\nservices:\n  peripheral-manager-usb:\n    <<: *common\n    image: nuvlabox/peripheral-manager-usb:1.1.0\n    restart: on-failure\n    network_mode: host\n    environment:\n      - NUVLABOX_UUID=${NUVLABOX_UUID}\n      - NUVLA_ENDPOINT=${NUVLA_ENDPOINT:-nuvla.io}\n      - NUVLA_ENDPOINT_INSECURE=${NUVLA_ENDPOINT_INSECURE:-False}\n    volumes:\n      - /dev:/dev:ro\n      - /run/udev/control:/run/udev/control:ro\n      - nuvlabox-db:/srv/nuvlabox/shared\n"
    }, {
      "name" : "docker-compose.yml",
      "scope" : "",
      "file" : "version: \"3.6\"\n\nx-common: &common\n  stop_grace_period: 4s\n  logging:\n    options:\n      max-size: \"250k\"\n      max-file: \"10\"\n  labels:\n    - \"nuvlabox.component=True\"\n    - \"nuvlabox.deployment=production\"\n\nvolumes:\n  nuvlabox-db:\n    driver: local\n\nnetworks:\n  nuvlabox-shared-network:\n    driver: overlay\n    name: nuvlabox-shared-network\n    attachable: true\n\nservices:\n  data-gateway:\n    <<: *common\n    image: traefik:2.1.1\n    container_name: datagateway\n    restart: on-failure\n    command:\n      - --entrypoints.mqtt.address=:1883\n      - --entrypoints.web.address=:80\n      - --providers.docker=true\n      - --providers.docker.exposedbydefault=false\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n    networks:\n      - default\n      - nuvlabox-shared-network\n\n  nb-mosquitto:\n    <<: *common\n    image: eclipse-mosquitto:1.6.8\n    container_name: nbmosquitto\n    restart: always\n    labels:\n      - \"traefik.enable=true\"\n      - \"traefik.tcp.routers.mytcprouter.rule=HostSNI(`*`)\"\n      - \"traefik.tcp.routers.mytcprouter.entrypoints=mqtt\"\n      - \"traefik.tcp.routers.mytcprouter.service=mosquitto\"\n      - \"traefik.tcp.services.mosquitto.loadbalancer.server.port=1883\"\n      - \"nuvlabox.component=True\"\n      - \"nuvlabox.deployment=production\"\n    healthcheck:\n      test: [\"CMD-SHELL\", \"timeout -t 5 mosquitto_sub -t '$$SYS/#' -C 1 | grep -v Error || exit 1\"]\n      interval: 10s\n      timeout: 10s\n      start_period: 10s\n\n  system-manager:\n    <<: *common\n    image: nuvlabox/system-manager:1.2.0\n    restart: always\n    environment:\n      - SKIP_MINIMUM_REQUIREMENTS=False\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n      - nuvlabox-db:/srv/nuvlabox/shared\n    ports:\n      - 127.0.0.1:3636:3636\n    healthcheck:\n      test: [\"CMD\", \"curl\", \"-f\", \"http://localhost:3636\"]\n      interval: 30s\n      timeout: 10s\n      retries: 4\n      start_period: 10s\n\n  agent:\n    <<: *common\n    image: nuvlabox/agent:1.10.0\n    restart: on-failure\n    privileged: true\n    environment:\n      - NUVLABOX_UUID=${NUVLABOX_UUID}\n      - NUVLABOX_ENGINE_VERSION=1.11.0\n      - NUVLA_ENDPOINT=${NUVLA_ENDPOINT:-nuvla.io}\n      - NUVLA_ENDPOINT_INSECURE=${NUVLA_ENDPOINT_INSECURE:-False}\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n      - nuvlabox-db:/srv/nuvlabox/shared\n      - /:/rootfs:ro\n    expose:\n      - 5000\n    depends_on:\n      - system-manager\n      - compute-api\n\n  management-api:\n    <<: *common\n    image: nuvlabox/management-api:0.3.1\n    restart: on-failure\n    environment:\n      - NUVLA_ENDPOINT=${NUVLA_ENDPOINT:-nuvla.io}\n      - NUVLA_ENDPOINT_INSECURE=${NUVLA_ENDPOINT_INSECURE:-False}\n      - NUVLABOX_SSH_PUB_KEY=${NUVLABOX_SSH_PUB_KEY}\n      - HOST_USER=$USER\n    volumes:\n      - /proc/sysrq-trigger:/sysrq\n      - ${HOME}/.ssh/:/hostfs/.ssh/\n      - nuvlabox-db:/srv/nuvlabox/shared\n      - /var/run/docker.sock:/var/run/docker.sock\n    ports:\n      - 5001:5001\n    healthcheck:\n      test: curl -k https://localhost:5001 2>&1 | grep SSL\n      interval: 20s\n      timeout: 10s\n      start_period: 30s\n\n  compute-api:\n    <<: *common\n    image: nuvlabox/compute-api:1.0.0\n    container_name: compute-api\n    restart: on-failure\n    pid: \"host\"\n    environment:\n      - HOST=${HOSTNAME:-nuvlabox}\n    volumes:\n      - /var/run/docker.sock:/var/run/docker.sock\n      - nuvlabox-db:/srv/nuvlabox/shared\n    ports:\n      - 5000:5000\n    depends_on:\n      - system-manager\n\n  network-manager:\n    <<: *common\n    image: nuvlabox/network-manager:1.0.0\n    restart: on-failure\n    environment:\n      - NUVLABOX_UUID=${NUVLABOX_UUID}\n      - VPN_INTERFACE_NAME=${NUVLABOX_VPN_IFACE:-vpn}\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - system-manager\n\n  vpn-client:\n    <<: *common\n    image: nuvlabox/vpn-client:0.0.4\n    container_name: vpn-client\n    restart: always\n    network_mode: host\n    cap_add:\n      - NET_ADMIN\n    devices:\n      - /dev/net/tun\n    environment:\n      - NUVLABOX_UUID=${NUVLABOX_UUID}\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n    depends_on:\n      - network-manager\n\n  security:\n    <<: *common\n    image: nuvlabox/security:0.0.2\n    restart: on-failure\n    network_mode: host\n    environment:\n      - EXTERNAL_CVE_VULNERABILITY_DB=${EXTERNAL_CSV_VULNERABILITY_DB:-\"https://github.com/nuvla/vuln-db/blob/main/databases/all.aggregated.csv.gz?raw=true\"}\n      - EXTERNAL_CVE_VULNERABILITY_DB_UPDATE_INTERVAL=${EXTERNAL_CVE_VULNERABILITY_DB_UPDATE_INTERVAL:-86400}\n      - SECURITY_SCAN_INTERVAL=${SECURITY_SCAN_INTERVAL:-300}\n      - NUVLA_ENDPOINT=${NUVLA_ENDPOINT:-nuvla.io}\n      - NUVLA_ENDPOINT_INSECURE=${NUVLA_ENDPOINT_INSECURE:-False}\n    volumes:\n      - nuvlabox-db:/srv/nuvlabox/shared\n"
    }, {
      "name" : "install.sh",
      "scope" : "",
      "file" : "#!/bin/sh\n\n# NuvlaBox Engine advanced installation script\n# This script is an alternative for the conventional one-command `docker-compose ... ` installation/halt/remove methods\n# It provides extra checks and guidance for making sure that:\n#  1. there are no existing NuvlaBox Engines already running\n#  2. handle existing installations before installing a new one\n#  3. checks installation requirements\n#  4. installs/updates/removes NuvlaBox Engine\n\ncompose_files=\"docker-compose.yml\"\nstrategies=\"UPDATE OVERWRITE\"\nstrategy=\"UPDATE\"\nactions=\"INSTALL REMOVE HALT\"\naction=\"INSTALL\"\nextra_env=\"\"\nenv_file=\"\"\n\nusage()\n{\n    echo \"NuvlaBox Engine advanced installation wrapper\"\n    echo \"\"\n    echo \"./install.sh\"\n    echo \"\"\n    echo \" -h --help\"\n    echo \" --environment=KEY1=value1,KEY2=value2\\t\\t(optional) Comma-separated environment keypair values\"\n    echo \" --env-path=PATH\\t\\t\\t\\t(optional) Path for env file\"\n    echo \" --compose-files=file1.yml,file2.yml\\t\\t(optional) Comma-separated list of compose files to deploy. Default: ${compose_files}\"\n    echo \" --installation-strategy=STRING\\t\\t\\t(optional) Strategy when action=INSTALL. Must be on of: ${strategies}. Default: ${strategy}\"\n    echo \"\\t\\t UPDATE - if NuvlaBox Engine is already running, replace outdated components and start stopped ones. Otherwise, install\"\n    echo \"\\t\\t OVERWRITE - if NuvlaBox Engine is already running, shut it down and re-install. Otherwise, install\"\n    echo \" --action=STRING\\t\\t\\t\\t(optional) What action to take. Must be on of: ${actions}. Default: ${action}\"\n    echo \"\\t\\t INSTALL - runs 'docker-compose up'\"\n    echo \"\\t\\t REMOVE - removes the NuvlaBox Engine and all associated data. Same as 'docker-compose down -v\"\n    echo \"\\t\\t HALT - shuts down the NuvlaBox Engine but keeps data, so it can be revived later. Same as 'docker-compose down\"\n    echo \"\"\n}\n\nwhile [ \"$1\" != \"\" ]; do\n    PARAM=`echo $1 | awk -F= '{print $1}'`\n    VALUE=`echo $1 | cut -d \"=\" -f 2-`\n    case $PARAM in\n        -h | --help)\n            usage\n            exit\n            ;;\n        --environment)\n            extra_env=$VALUE\n            ;;\n        --env-file)\n            env_file=$VALUE\n            ;;\n        --compose-files)\n            compose_files=$VALUE\n            ;;\n        --installation-strategy)\n            strategy=$VALUE\n            ;;\n        --action)\n            action=$VALUE\n            ;;\n        *)\n            echo \"ERROR: unknown parameter \\\"$PARAM\\\"\"\n            usage\n            exit 1\n            ;;\n    esac\n    shift\ndone\n\nwhich docker-compose >/dev/null\nif [ $? -ne 0 ]\nthen\n  echo \"ERR: docker-compose is not installed. Cannot continue\"\n  exit 1\nfi\n\nset -x\n\nif [ ! -z \"${extra_env}\" ]\nthen\n  echo \"Setting up environment ${extra_env}\"\n  export $(echo ${extra_env} | tr ',' ' ') &>/dev/null\nfi\n\ncommand_compose_files=\"\"\nfor file in $(echo ${compose_files} | tr ',' '\\n')\ndo\n  command_compose_files=\"${command_compose_files} -f ${file}\"\ndone\n\ncommand_env=\"\"\nif [ ! -z \"${env_file}\" ]\nthen\n  command_env=\"${command_env} --env-file ${env_file}\"\nfi\n\nif [ \"${action}\" = \"REMOVE\" ]\nthen\n  echo \"INFO: removing NuvlaBox installation completely\"\n  docker-compose -p nuvlabox ${command_compose_files} ${command_env} down -v\n  ([ ! -z \"${env_file}\" ] && rm \"${env_file}\")\nelif [ \"${action}\" = \"HALT\" ]\nthen\n  echo \"INFO: halting NuvlaBox. You can bring it back later by simply re-installing with the same parameters as before\"\n  docker-compose -p nuvlabox ${command_compose_files} ${command_env} down\nelif [ \"${action}\" = \"INSTALL\" ]\nthen\n  if [ \"${strategy}\" = \"UPDATE\" ]\n  then\n    existing_projects=$(docker-compose -p nuvlabox ${command_compose_files} ${command_env} ps -a -q)\n    if [ ! -z \"${existing_projects}\" ]\n    then\n      echo \"INFO: found an active NuvlaBox installation. Updating it\"\n    else\n      echo \"INFO: no active NuvlaBox installations found. Installing from scratch\"\n    fi\n    docker-compose -p nuvlabox ${command_compose_files} ${command_env} up -d\n  elif [ \"${strategy}\" = \"OVERWRITE\" ]\n  then\n    echo \"WARNING: about to delete any existing NuvlaBox installations...press Ctrl+c in the next 5 seconds to stop\"\n    sleep 5\n    docker-compose -p nuvlabox ${command_compose_files} ${command_env} down -v --remove-orphans\n    echo \"INFO: installing NuvlaBox Engine from scratch\"\n    docker-compose -p nuvlabox ${command_compose_files} ${command_env} up -d\n  else\n    echo \"WARNING: strategy ${strategy} not recognized. Use -h for help. Nothing to do\"\n  fi\nelse\n  echo \"WARNING: action ${action} not recognized. Use -h for help. Nothing to do\"\n  exit 0\nfi"
    } ],
    "updated-by" : "user/7644d34d-4c59-47d5-bdde-6d4c3deedb82",
    "created-by" : "group/nuvla-admin",
    "id" : "nuvlabox-release/228ddb9e-d09b-48d0-9999-d53e9ea6d72e",
    "url" : "https://github.com/nuvlabox/deployment/releases/tag/1.11.0",
    "resource-type" : "nuvlabox-release",
    "acl" : {
      "view-meta" : [ "group/nuvla-user" ],
      "view-data" : [ "group/nuvla-user" ],
      "owners" : [ "group/nuvla-admin" ]
    },
    "operations" : [ {
      "rel" : "edit",
      "href" : "nuvlabox-release/228ddb9e-d09b-48d0-9999-d53e9ea6d72e"
    }, {
      "rel" : "delete",
      "href" : "nuvlabox-release/228ddb9e-d09b-48d0-9999-d53e9ea6d72e"
    } ],
    "published" : false
  } ],
  "operations" : [ {
    "rel" : "add",
    "href" : "nuvlabox-release"
  }, {
    "rel" : "bulk-delete",
    "href" : "nuvlabox-release"
  } ]
});