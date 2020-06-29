#!/usr/bin/env python3

# -*- coding: utf-8 -*-

"""NuvlaBox Self Registration script

This script is part of the NuvlaBox industrialization process.
Given the right user credentials and NuvlaBox initialization attributes,
this script will automatically register a new NuvlaBox resource in Nuvla.

Arguments:
:param nuvlabox-installation-trigger-json: JSON string of the NuvlaBox Installation Trigger's content. See schema below

The expected JSON schema is:
{
  "apikey": "credential/<uuid>",
  "apisecret": "<secret>",
  "endpoint": "<nuvla endpoint>",
  "version": "<nuvlabox-engine release>",
  "script": "<link to this script>",
  "name": "<basename nuvlaboxes>",
  "description": "<base description>",
  "vpn": "infrastructure-service/<uuid>",
  "assets": ["docker-compose.yml", <other compose files to install alongside>],
  "ssh": {
            "ids": ["credential/111-bbb-ccc", ...],
            "public-keys": ["ssh-rsa AAA...", ...]
            }
}

:returns NuvlaBox UUID
"""

import requests
import argparse
import json
import time
import os
from subprocess import run, PIPE, STDOUT, TimeoutExpired
from uuid import getnode as get_mac

__copyright__ = "Copyright (C) 2020 SixSq"
__email__ = "support@sixsq.com"


def arguments():
    """ Builds a generic argparse

    :return: parser
    """

    workdir = '/opt/nuvlabox/installation'

    parser = argparse.ArgumentParser(description='NuvlaBox Agent')
    parser.add_argument('--nuvlabox-installation-trigger-json', dest='nb_trigger_content', default=None, metavar='JSON',
                        help="JSON content, as a string, of the NuvlaBox installation USB trigger file")
    parser.add_argument('--nuvlabox-installation-dir', dest='nb_workdir', default=workdir, metavar='PATH',
                        help="Location on the filesystem where to keep the NuvlaBox Engine installation files")

    return parser.parse_args()


def prepare_nuvlabox_engine_installation(version, compose_files, workdir, keep_files=[]):
    """ Prepares the working environment for installing the NuvlaBox Engine

    :param version: GitHub release of the NuvlaBox Engine
    :param compose_files: list of release assets to download
    :param workdir: path where the compose files are to be saved
    :param keep_files: list of files that is not supposed to be modified during this preparation

    :returns absolute path to the NuvlaBox Engine installer script
    """
    github_release = 'https://github.com/nuvlabox/deployment/releases/download/{}'.format(version)

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


def install_nuvlabox_engine(cmd, env=os.environ.copy(), timeout=600):
    """ Runs a command

    :param cmd: command to be executed
    :param env: environment to be passed
    :param timeout: time after which the command will abruptly be terminated
    """

    try:
        result = run(cmd, stdout=PIPE, stderr=STDOUT, env=env, input=None,
                     timeout=timeout, encoding='UTF-8')

    except TimeoutExpired:
        raise Exception('Command execution timed out after {} seconds'.format(timeout))

    if result.returncode != 0:
        raise Exception(result.stdout)

    print(result.stdout)


if __name__ == "__main__":
    args = arguments()

    nb_trigger_json = json.loads(args.nb_trigger_content)
    nb_workdir = args.nb_workdir.rstrip('/')
    env_file = "{}/.env".format(nb_workdir)

    # Check if env files already exists
    # cause that will tell us if this is the first time we are self-registring this NB or not
    # if there's a previous env file (thus previous installation), we will check if it is COMMISSIONED or not
    # based on this check, we will either UPDATE or OVERWRITE the existing installation, respectively
    installation_strategy = "OVERWRITE"    # default
    nuvlabox_id = None
    previous_conf = {}
    new_conf = {}
    if not os.path.exists(nb_workdir):
        os.makedirs(nb_workdir)
    else:
        if os.path.isfile(env_file):
            # .env file exists - get the previous details
            with open(env_file) as f:
                for l in f.read().splitlines():
                    if l and "=" in l:
                        varname = l.split('=', 1)[0]
                        varvalue = l.split('=', 1)[1]
                        previous_conf[varname] = varvalue

    # argparse
    nuvla = nb_trigger_json['endpoint']
    nuvla_endpoint = nb_trigger_json['endpoint'].rstrip('/').rstrip('/api') + "/api"
    nb_basename = nb_trigger_json.get('name', '')
    nb_basedescription = nb_trigger_json.get('description', ' ')
    nb_release = nb_trigger_json['version']
    nb_vpn_server_id = nb_trigger_json.get('vpn')
    nb_assets = nb_trigger_json['assets']
    nb_ssh = nb_trigger_json.get('ssh', {})

    nb_ssh_pubkeys = nb_ssh.get('public-keys', [])
    nb_version = nb_release.split('.')[0]

    login_apikey = {
        "template": {
            "href": "session-template/api-key",
            "key": nb_trigger_json['apikey'],
            "secret": nb_trigger_json['apisecret']
        }
    }

    s = requests.Session()

    # login
    connection_verify = True
    login_endpoint = nuvla_endpoint + "/session"
    print("Nuvla login at {}...".format(login_endpoint))
    try:
        session = s.post(login_endpoint, json=login_apikey)
    except requests.exceptions.SSLError:
        connection_verify = False
        session = s.post(login_endpoint, json=login_apikey, verify=connection_verify)

    session.raise_for_status()

    new_conf['NUVLA_ENDPOINT'] = nuvla
    new_conf['NUVLA_ENDPOINT_INSECURE'] = str(not connection_verify)
    if nb_ssh_pubkeys:
        new_conf['NUVLABOX_SSH_PUB_KEY'] = '\\n'.join(nb_ssh_pubkeys)

    if previous_conf:
        if "NUVLABOX_UUID" in previous_conf:
            previous_uuid = previous_conf['NUVLABOX_UUID']
            print("Existing env file from previous deployment found, with NuvlaBox UUID {}".format(previous_uuid))
            check_nb_endpoint = nuvla_endpoint + "/" + previous_uuid
            nb = s.get(check_nb_endpoint, verify=connection_verify)
            if nb.status_code == 200:
                state = nb.json().get('state', 'UNKNOWN')
                if state in ["DECOMMISSIONED", 'ERROR']:
                    # this NuvlaBox has been decommissioned or is in error, just overwrite the local installation
                    print("Previous NuvlaBox {} is in state {}. Going to OVERWRITE it...".format(previous_uuid, state))
                else:
                    new_conf['NUVLABOX_UUID'] = previous_uuid
                    if new_conf == previous_conf:
                        print("NuvlaBox environment hasn't changed, performing an UPDATE")
                        installation_strategy = "UPDATE"
                    else:
                        print("NuvlaBox environment different from existing installation, performing an OVERWRITE")
            elif nb.status_code == 404:
                # doesn't exist, so let's just OVERWRITE this local installation
                print("Previous NuvlaBox {} doesn't exist anymore...creating new one".format(previous_uuid))
            else:
                # something went wrong, either a network issue or we have the wrong credentials to access the
                # current NuvlaBox resource...just throw the error and do nothing
                nb.raise_for_status()
        else:
            print("There's a previous NuvlaBox environment but couldn't find a NuvlaBox UUID...let's OVERWRITE")

    if installation_strategy == "OVERWRITE":
        print("Creating new NuvlaBox resource...")
        try:
            unique_id = str(get_mac())
        except:
            unique_id = str(int(time.time()))

        nb_name = nb_basename.rstrip("_") + "_" + unique_id if nb_basename else unique_id

        nb_description = "{} - self-registration number {}".format(nb_basedescription, unique_id)

        nuvlabox = {
            "name": nb_name,
            "description": nb_description,
            "version": int(nb_version)
        }

        if nb_vpn_server_id:
            nuvlabox['vpn-server-id'] = nb_vpn_server_id

        if nb_ssh and "ids" in nb_ssh and isinstance(nb_ssh.get('ids'), list):
            nuvlabox['ssh-keys'] = nb_ssh.get('ids')

        new_nb_endpoint = nuvla_endpoint + "/nuvlabox"
        nb_id = s.post(new_nb_endpoint, json=nuvlabox, verify=connection_verify)

        nb_id.raise_for_status()

        nuvlabox_id = nb_id.json()["resource-id"]
        print("Created NuvlaBox resource {} in {}".format(nuvlabox_id, nuvla))

        new_conf['NUVLABOX_UUID'] = nuvlabox_id

    # update env file
    print("Setting up environment {} at {}".format(new_conf, env_file))
    with open(env_file, 'w') as f:
        for varname, varvalue in new_conf.items():
            f.write("{}={}\n".format(varname, varvalue))

    try:
        installer_file, compose_files = prepare_nuvlabox_engine_installation(nb_release,
                                                                             nb_assets,
                                                                             nb_workdir,
                                                                             keep_files=[env_file])

        install_command = ["sh", installer_file, "--env-file={}".format(env_file),
                           "--compose-files={}".format(",".join(compose_files)),
                           "--installation-strategy={}".format(installation_strategy), "--action=INSTALL"]

        print("Installing NuvlaBox Engine - this can take a few minutes...")
        install_nuvlabox_engine(install_command)
    except:
        # On any error, cleanup the resource in Nuvla
        print("NuvlaBox Engine installation failed")
        if nuvlabox_id:
            print("removing {} from Nuvla".format(nuvlabox_id))
            s.delete(nuvla_endpoint + "/" + nuvlabox_id, verify=connection_verify)
        raise
