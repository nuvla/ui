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

    print(result.stdout)


if __name__ == "__main__":
    args = arguments()

    nb_trigger_json = json.loads(args.nb_trigger_content)
    nb_workdir = args.nb_workdir.rstrip('/')
    env_file = "{}/.env"

    # Check if env files already exists
    # cause that will tell us if this is the first time we are self-registring this NB or not
    # if there's a previous env file (thus previous installation), we will check if it is COMMISSIONED or not
    # based on this check, we will either UPDATE or OVERWRITE the existing installation, respectively
    installation_strategy = "UPDATE"    # default
    previous_conf = {}
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
    nb_basename = nb_trigger_json.get('name', '').rstrip('_')
    nb_basedescription = nb_trigger_json.get('description', '')
    nb_release = nb_trigger_json['version']
    nb_vpn_server_id = nb_trigger_json.get('vpn')
    nb_assets = nb_trigger_json['assets']

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
    try:
        session = s.post(login_endpoint, json=login_apikey)
    except requests.exceptions.SSLError:
        connection_verify = False
        session = s.post(login_endpoint, json=login_apikey, verify=connection_verify)

    session.raise_for_status()

    # Double check previous conf
    new_conf = previous_conf.copy()
    if previous_conf:
        if "NUVLA_ENDPOINT" not in previous_conf or previous_conf['NUVLA_ENDPOINT'] != nuvla:
            # new NUVLA_ENDPOINT, therefore, we are NOT updating the current installation
            installation_strategy = "OVERWRITE"
            new_conf['NUVLA_ENDPOINT'] = nuvla
            new_conf['NUVLA_ENDPOINT_INSECURE'] = not connection_verify
        else:
            # the NUVLA_ENDPOINT is the same as the previous installation
            # so let's double check if the current installation is still COMMISSIONED
            if "NUVLABOX_UUID" in previous_conf:
                check_nb_endpoint = nuvla_endpoint + "/" + previous_conf['NUVLABOX_UUID']
                nb = s.get(check_nb_endpoint, verify=connection_verify)
                if nb.status_code == 200:
                    state = nb.json().get('state', 'UNKNOWN')
                    if state in ["DECOMMISSIONED", 'ERROR']:
                        # this NuvlaBox has been decommissioned or is in error, just overwrite the local installation
                        installation_strategy = "OVERWRITE"
                elif nb.status_code == 404:
                    # doesn't exist, so let's just OVERWRITE this local installation
                    installation_strategy = "OVERWRITE"
                else:
                    # something went wrong, either a network issue or we have the wrong credentials to access the
                    # current NuvlaBox resource...just throw the error and do nothing
                    nb.raise_for_status()
            else:
                # there is not UUID from a previous installation, so something went wrong, let's re-install
                installation_strategy = "OVERWRITE"

    nuvlabox_id = None
    if installation_strategy == "OVERWRITE":
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

        new_conf['NUVLABOX_UUID'] = nuvlabox_id

        # update new env file
        with open(env_file, 'w') as f:
            for varname, varvalue in new_conf:
                f.write("{}={}\n".format(varname, varvalue))

    try:
        installer_file, compose_files = prepare_nuvlabox_engine_installation(nb_release, nb_assets, nb_workdir)

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
