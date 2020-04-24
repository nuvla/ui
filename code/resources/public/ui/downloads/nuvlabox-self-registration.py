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

    return parser


if __name__ == "__main__":
    args = arguments()

    nb_trigger_json = json.loads(args.nb_trigger_content)

    nuvla_endpoint = nb_trigger_json['endpoint'].rstrip('/')
    nb_basename = nb_trigger_json['name'].rstrip('_')
    nb_basedescription = nb_trigger_json['description']

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
        s.post(login_endpoint, json=login_apikey)
    except requests.exceptions.SSLError:
        s.post(login_endpoint, json=login_apikey, verify=False)

    # create Nuvlabox
    try:
        unique_id = str(get_mac())
    except:
        unique_id = str(int(time.time()))

    nb_name = nb_basename + "_" + unique_id
    nb_description = "{} - {}".format(nb_basedescription, unique_id)
