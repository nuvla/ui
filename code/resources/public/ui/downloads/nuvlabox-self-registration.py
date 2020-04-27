#!/usr/local/bin/python
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
from uuid import getnode as get_mac

__copyright__ = "Copyright (C) 2020 SixSq"
__email__ = "support@sixsq.com"


def arguments():
    """ Builds a generic argparse

    :return: parser
    """

    parser = argparse.ArgumentParser(description='NuvlaBox Agent')
    parser.add_argument('--nuvlabox-installation-trigger-json', dest='nb_trigger_content', default=None, metavar='JSON')

    return parser.parse_args()


if __name__ == "__main__":
    args = arguments()

    nb_trigger_json = json.loads(args.nb_trigger_content)

    nuvla_endpoint = nb_trigger_json['endpoint'].rstrip('/').rstrip('/api') + "/api"
    nb_basename = nb_trigger_json['name'].rstrip('_')
    nb_basedescription = nb_trigger_json['description']
    nb_version = nb_trigger_json['version'].split('.')[0]
    nb_vpn_server_id = nb_trigger_json.get('vpn')

    login_apikey = {
        "template": {
            "href": "session-template/api-key",
            "key": nb_trigger_json['apikey'],
            "secret": nb_trigger_json['apisecret']
        }
    }

    s = requests.Session()

    # login
    login_endpoint = nuvla_endpoint + "/session"
    try:
        session = s.post(login_endpoint, json=login_apikey).json()
    except requests.exceptions.SSLError:
        session = s.post(login_endpoint, json=login_apikey, verify=False).json()

    if session["status"] != 201:
        raise Exception("Unable to login at {}. Reason: {}".format(login_endpoint, session['message']))

    # create Nuvlabox
    try:
        unique_id = str(get_mac())
    except:
        unique_id = str(int(time.time()))

    nb_name = nb_basename + "_" + unique_id
    nb_description = "{} - {}".format(nb_basedescription, unique_id)

    nuvlabox = {
        "name": nb_name,
        "description": nb_description,
        "version": int(nb_version)
    }

    if nb_vpn_server_id:
        nuvlabox['vpn-server-id'] = nb_vpn_server_id

    new_nb_endpoint = nuvla_endpoint + "/nuvlabox"
    try:
        nb_id = s.post(new_nb_endpoint, json=nuvlabox).json()
    except requests.exceptions.SSLError:
        nb_id = s.post(new_nb_endpoint, json=nuvlabox, verify=False).json()

    if nb_id['status'] != 201:
        raise Exception("Failed to register new NuvlaBox at {}. Reason: {}".format(new_nb_endpoint, nb_id['message']))

    print(nb_id["resource-id"])
