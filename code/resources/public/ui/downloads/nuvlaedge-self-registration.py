#!/usr/bin/env python3

# -*- coding: utf-8 -*-

"""NuvlaEdge Self Registration script

This script is part of the NuvlaEdge industrialization process.
Given the right user credentials and NuvlaEdge initialization attributes,
this script will automatically register a new NuvlaEdge resource in Nuvla.

Arguments:
:param nuvlaedge-installation-trigger-json: JSON string of the NuvlaEdge Installation Trigger's content. See schema below

The expected JSON schema is:
{
  "apikey": "credential/<uuid>",
  "apisecret": "<secret>",
  "endpoint": "<nuvla endpoint>",
  "version": "<nuvlaedge release>",
  "script": "<link to this script>",
  "name": "<basename nuvlaedges>",
  "description": "<base description>",
  "vpn": "infrastructure-service/<uuid>",
  "assets": ["docker-compose.yml", <other compose files to install alongside>],
  "environment": {
            "HOSTNAME": "myhostname",
            "SKIP_MINIMUM_REQUIREMENTS": True
            },
  "ssh": {
            "ids": ["credential/111-bbb-ccc", ...],
            "public-keys": ["ssh-rsa AAA...", ...]
            }
}

:returns NuvlaEdge UUID
"""

import argparse
import datetime
import json
import platform
import time
import uuid
import os

from subprocess import run, PIPE, STDOUT, TimeoutExpired

import requests


__copyright__ = "Copyright (C) 2020 SixSq"
__email__ = "support@sixsq.com"


def arguments():
    """ Builds a generic argparse

    :return: parser
    """

    workdir = '/opt/nuvlaedge/installation'
    workdir_old = '/opt/nuvlabox/installation'

    parser = argparse.ArgumentParser(description='NuvlaEdge self-registration')

    parser.add_argument('--nuvlaedge-installation-trigger-json',
                        '--nuvlabox-installation-trigger-json',
                        dest='nuvlaedge_trigger_content', default=None, metavar='JSON',
                        help="JSON content, as a string, of the NuvlaEdge installation trigger file")

    parser.add_argument('--nuvlaedge-installation-dir',
                        '--nuvlabox-installation-dir',
                        dest='nuvlaedge_workdir', default=workdir, metavar='PATH',
                        help="Location on the filesystem where to keep the NuvlaEdge Engine installation files")

    parser.add_argument('--nuvlaedge-old-installation-dir',
                        '--nuvlabox-old-installation-dir',
                        dest='nuvlaedge_old_workdir', default=workdir_old, metavar='PATH',
                        help="Location on the filesystem where the previous NuvlaEdge Engine installation files were located")

    return parser.parse_args()


def get_mac():
    h = hex(uuid.getnode())[-12:]
    return ':'.join(h[i:i+2] for i in range(0,12,2))


def is_true(s, **kwargs):
    """ Check if 's' string represent True else False

    :param s: String to check
    :param default: If the check cannot be done return this value or raise ValueError if not provided.

    :returns True if 's' string represent True else False
    """
    try:
        return s if isinstance(s, bool) \
            else bool(s and s.lower() in ['true', '1', 't', 'y', 'yes'])
    except:
        message = f'Cannot check if "{s}" is True'
        if 'default' not in kwargs:
            raise ValueError(message)
        else:
            return kwargs['default']


def prepare_nuvlaedge_installation(version, compose_files, workdir, keep_files=[]):
    """ Prepares the working environment for installing the NuvlaEdge Engine

    :param version: version number of NuvlaEdge to install
    :param compose_files: list of release assets to download
    :param workdir: path where the compose files are to be saved
    :param keep_files: list of files that are not supposed to be modified during this preparation

    :returns absolute path to the NuvlaEdge Engine installer script
    """
    github_release = 'https://github.com/nuvlaedge/deployment/releases/download/{}'.format(version)

    # Double check that the workdir is created
    try:
        # Create working directory
        os.makedirs(workdir)
    except FileExistsError:
        pass

    # Backup the previous installation files
    existing_files = os.listdir(workdir)
    now = int(time.time())
    for efile in existing_files:
        filename = "{}/{}".format(workdir, efile)
        if not filename.endswith("backup") and filename not in keep_files:
            new_file = "{}.backup".format(filename, now)
            os.rename(filename, new_file)

    final_compose_files = []
    for file in compose_files:
        gh_url = "{}/{}".format(github_release, file)

        r = requests.get(gh_url)
        r.raise_for_status()
        save_compose_file_at = "{}/{}".format(workdir, file)
        with open(save_compose_file_at, 'wb') as f:
            f.write(r.content)

        final_compose_files.append(save_compose_file_at)

    # also download install file
    installer_file_name = "install.sh"
    installer_file = "{}/{}".format(workdir, installer_file_name)
    installer_file_gh = "{}/{}".format(github_release, installer_file_name)

    r = requests.get(installer_file_gh)
    r.raise_for_status()
    with open(installer_file, 'wb') as f:
        f.write(r.content)

    return installer_file, final_compose_files


def install_nuvlaedge(cmd, env=os.environ.copy(), timeout=600):
    """ Runs a command

    :param cmd: command to be executed
    :param env: environment to be passed
    :param timeout: time after which the command will abruptly be terminated
    """
    try:
        result = run(cmd, stdout=PIPE, stderr=STDOUT, env=env, input=None,
                     timeout=timeout, universal_newlines=True)

    except TimeoutExpired:
        raise Exception('Command execution timed out after {} seconds'.format(timeout))

    if result.returncode != 0:
        raise Exception(result.stdout)

    print(result.stdout)


def nuvla_login(api_endpoint, api_key, api_secret, insecure=False):
    session = requests.Session()
    session.verify = not insecure
    login_endpoint = api_endpoint + "/session"
    payload = {
        "template": {
            "href": "session-template/api-key",
            "key": api_key,
            "secret": api_secret
        }
    }
    print("Nuvla login at {}...".format(login_endpoint))
    response = session.post(login_endpoint, json=payload)
    response.raise_for_status()
    return session


def read_env_file(env_file):
    env_vars = []
    with open(env_file) as f:
        for line in f.read().splitlines():
            if line and "=" in line:
                name, value = line.split('=', 1)
                env_vars[name] = value
    return env_vars


def generate_nuvlaedge_name_description(name_prefix, description_prefix):
    name_suffix = ''

    mac = ''
    try:
        mac = get_mac()
        name_suffix = '{} {}'.format(name_suffix, mac).strip()
    except:
        pass

    isotime = ''
    try:
        isotime = datetime.datetime.now().isoformat(' ')[:19]
        name_suffix = '{} {}'.format(name_suffix, isotime).strip()
    except:
        pass

    hostname = platform.node()
    name_suffix = '{} {}'.format(name_suffix, hostname).strip()

    name = "{} - {}".format(name_prefix, name_suffix) if name_prefix else name_suffix
    description = "{} - MAC address: {} - Installation time: {} - Hostname: {}".format(description_prefix, mac, isotime, hostname)

    return name, description


if __name__ == "__main__":
    args = arguments()

    ne_trigger_json = json.loads(args.nuvlaedge_trigger_content)
    nuvlaedge_workdir = args.nuvlaedge_workdir.rstrip('/')
    nuvlaedge_old_workdir = args.nuvlaedge_old_workdir.rstrip('/')
    env_file = "{}/.env".format(nuvlaedge_workdir)
    old_env_file = "{}/.env".format(nuvlaedge_old_workdir)

    installation_strategy = "OVERWRITE" # default
    nuvlaedge_id = None

    if not os.path.exists(nuvlaedge_workdir):
        os.makedirs(nuvlaedge_workdir)

    new_conf = ne_trigger_json.get('environment', {})
    nuvla_endpoint     = ne_trigger_json['endpoint']
    nuvla_api_endpoint = nuvla_endpoint.rstrip('/').rstrip('/api') + "/api"
    nuvla_api_key      = ne_trigger_json['apikey']
    nuvla_api_secret   = ne_trigger_json['apisecret']
    nuvla_api_insecure = is_true(new_conf.get('NUVLA_ENDPOINT_INSECURE'), default=False)
    nuvlaedge_release         = ne_trigger_json['version']
    nuvlaedge_assets          = ne_trigger_json['assets']
    nuvlaedge_basename        = ne_trigger_json.get('name', '')
    nuvlaedge_basedescription = ne_trigger_json.get('description', ' ')
    nuvlaedge_vpn_server_id   = ne_trigger_json.get('vpn')
    nuvlaedge_ssh             = ne_trigger_json.get('ssh', {})
    nuvlaedge_ssh_pubkeys = nuvlaedge_ssh.get('public-keys', [])
    nuvlaedge_version     = nuvlaedge_release.split('.')[0]

    api = nuvla_login(nuvla_api_endpoint, nuvla_api_key, nuvla_api_secret, nuvla_api_insecure)

    new_conf['NUVLA_ENDPOINT'] = nuvla_endpoint
    new_conf['NUVLA_ENDPOINT_INSECURE'] = str(nuvla_api_insecure)
    if nuvlaedge_ssh_pubkeys:
        new_conf['NUVLABOX_SSH_PUB_KEY']  = '\\n'.join(nuvlaedge_ssh_pubkeys)
        new_conf['NUVLAEDGE_SSH_PUB_KEY'] = '\\n'.join(nuvlaedge_ssh_pubkeys)

    # Check if env files already exists
    # cause that will tell us if this is the first time we are self-registring this NB or not
    # if there's a previous env file (thus previous installation), we will check if it is COMMISSIONED or not
    # based on this check, we will either UPDATE or OVERWRITE the existing installation, respectively

    previous_conf = {}
    if os.path.isfile(env_file):
        previous_conf.update(read_env_file(env_file))
    elif os.path.isfile(old_env_file):
        previous_conf.update(read_env_file(old_env_file))

    if previous_conf:
        if "NUVLAEDGE_UUID" in previous_conf or "NUVLABOX_UUID" in previous_conf:
            previous_uuid = previous_conf.get('NUVLAEDGE_UUID', previous_conf.get("NUVLABOX_UUID"))
            print("Existing env file from previous deployment found, with NuvlaEdge UUID {}".format(previous_uuid))
            nuvlaedge_url = nuvla_api_endpoint + "/" + previous_uuid
            response = api.get(nuvlaedge_url)
            if response.status_code == 200:
                nuvlaedge = response.json()
                state = nuvlaedge.get('state', 'UNKNOWN')
                if state in ["DECOMMISSIONED", 'ERROR']:
                    # this NuvlaEdge has been decommissioned or is in error, just overwrite the local installation
                    print("Previous NuvlaEdge {} is in state {}. Going to OVERWRITE it...".format(previous_uuid, state))
                else:
                    new_conf['NUVLABOX_UUID'] = previous_uuid
                    new_conf['NUVLAEDGE_UUID'] = previous_uuid
                    if new_conf == previous_conf:
                        print("NuvlaEdge environment hasn't changed, performing an UPDATE")
                        installation_strategy = "UPDATE"
                    else:
                        print("NuvlaEdge environment different from existing installation, performing an OVERWRITE")
            elif response.status_code == 404:
                # doesn't exist, so let's just OVERWRITE this local installation
                print("Previous NuvlaEdge {} doesn't exist anymore...creating new one".format(previous_uuid))
            else:
                # something went wrong, either a network issue or we have the wrong credentials to access the
                # current NuvlaEdge resource...just throw the error and do nothing
                response.raise_for_status()
        else:
            print("There's a previous NuvlaEdge environment but couldn't find a NuvlaEdge UUID...let's OVERWRITE")

    if installation_strategy == "OVERWRITE":
        print("Creating new NuvlaEdge resource...")

        nuvlaedge_name, nuvlaedge_description = generate_nuvlaedge_name_description(nuvlaedge_basename, nuvlaedge_basedescription)

        nuvlaedge = {
            "name": nuvlaedge_name,
            "description": nuvlaedge_description,
            "version": int(nuvlaedge_version)
        }

        if nuvlaedge_vpn_server_id:
            nuvlaedge['vpn-server-id'] = nuvlaedge_vpn_server_id

        if nuvlaedge_ssh and "ids" in nuvlaedge_ssh and isinstance(nuvlaedge_ssh.get('ids'), list):
            nuvlaedge['ssh-keys'] = nuvlaedge_ssh.get('ids')

        new_nuvlaedge_endpoint = nuvla_api_endpoint + "/nuvlabox"
        response = api.post(new_nuvlaedge_endpoint, json=nuvlaedge)

        response.raise_for_status()

        nuvlaedge_id = response.json()["resource-id"]
        print("Created NuvlaEdge resource {} in {}".format(nuvlaedge_id, nuvla_endpoint))

        new_conf['NUVLABOX_UUID'] = nuvlaedge_id
        new_conf['NUVLAEDGE_UUID'] = nuvlaedge_id

    # update env file
    print("Setting up environment {} at {}".format(new_conf, env_file))
    with open(env_file, 'w') as f:
        for varname, varvalue in new_conf.items():
            f.write("{}={}\n".format(varname, varvalue))

    try:
        installer_file, compose_files = prepare_nuvlaedge_installation(nuvlaedge_release,
                                                                              nuvlaedge_assets,
                                                                              nuvlaedge_workdir,
                                                                              keep_files=[env_file])

        install_command = ["sh", installer_file,
                           "--env-file={}".format(env_file),
                           "--compose-files={}".format(",".join(compose_files)),
                           "--installation-strategy={}".format(installation_strategy),
                           "--action=INSTALL"]

        print("Installing NuvlaEdge Engine - this can take a few minutes...")
        install_nuvlaedge(install_command)
    except:
        # On any error, cleanup the resource in Nuvla
        print("NuvlaEdge Engine installation failed")
        if nuvlaedge_id:
            print("removing {} from Nuvla".format(nuvlaedge_id))
            api.delete(nuvla_api_endpoint + "/" + nuvlaedge_id)
        raise
