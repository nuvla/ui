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
  "assets": ["docker-compose.yml", <other compose files to install alongside>]
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


def prepare_nuvlabox_engine_installation(version, compose_files, workdir):
    """ Prepares the working environment for installing the NuvlaBox Engine

    :param version: GitHub release of the NuvlaBox Engine
    :param compose_files: list of release assets to download
    :param workdir: path where the compose files are to be saved

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
        if not efile.endswith("backup"):
            old_file = "{}/{}".format(workdir, efile)
            new_file = "{}/{}.{}.backup".format(workdir, efile, now)
            os.rename(old_file, new_file)

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


if __name__ == "__main__":
    args = arguments()

    environment = os.environ.copy()
    # We can also pass the env as an argument to the installer script later on, so let's save it
    environment_fallback = ""

    nb_trigger_json = json.loads(args.nb_trigger_content)
    nb_workdir = args.nb_workdir.rstrip('/')

    nuvla = nb_trigger_json['endpoint']
    environment_fallback += "NUVLA_ENDPOINT={}".format(nuvla)
    environment['NUVLA_ENDPOINT'] = nuvla

    nuvla_endpoint = nb_trigger_json['endpoint'].rstrip('/').rstrip('/api') + "/api"
    nb_basename = nb_trigger_json.get('name', '').rstrip('_')
    nb_basedescription = nb_trigger_json.get('description', '')
    nb_release = nb_trigger_json['version']
    nb_version = nb_release.split('.')[0]
    nb_vpn_server_id = nb_trigger_json.get('vpn')
    nb_assets = nb_trigger_json['assets']

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
    try:
        session = s.post(login_endpoint, json=login_apikey)
    except requests.exceptions.SSLError:
        environment_fallback += ",NUVLA_ENDPOINT_INSECURE=True"
        environment['NUVLA_ENDPOINT_INSECURE'] = "True"
        connection_verify = False
        session = s.post(login_endpoint, json=login_apikey, verify=connection_verify)

    session.raise_for_status()

    # create Nuvlabox
    try:
        unique_id = str(get_mac())
    except:
        unique_id = str(int(time.time()))

    nb_name = nb_basename + "_" + unique_id
    nb_description = "{}_With self-registration number {}".format(nb_basedescription, unique_id)

    nuvlabox = {
        "name": nb_name,
        "description": nb_description,
        "version": int(nb_version)
    }

    if nb_vpn_server_id:
        nuvlabox['vpn-server-id'] = nb_vpn_server_id

    new_nb_endpoint = nuvla_endpoint + "/nuvlabox"
    nb_id = s.post(new_nb_endpoint, json=nuvlabox, verify=connection_verify)

    nb_id.raise_for_status()

    nuvlabox_id = nb_id.json()["resource-id"]
    print("Created NuvlaBox resource {} in {}".format(nuvlabox_id, nuvla))
    environment_fallback += ",NUVLABOX_UUID={}".format(nuvlabox_id)
    environment['NUVLABOX_UUID'] = nuvlabox_id

    try:
        installer_file, compose_files = prepare_nuvlabox_engine_installation(nb_release, nb_assets, nb_workdir)

        install_command = ["sh", installer_file, "--environment={}".format(environment_fallback),
                           "--compose-files={}".format(",".join(compose_files)), "--installation-strategy=UPDATE",
                           "--action=INSTALL"]

        install_nuvlabox_engine(install_command, env=environment)
    except:
        # On any error, cleanup the resource in Nuvla
        print("NuvlaBox Engine installation failed - removing {} from Nuvla".format(nuvlabox_id))
        s.delete(nuvla_endpoint + "/" + nuvlabox_id, verify=connection_verify)
        raise
