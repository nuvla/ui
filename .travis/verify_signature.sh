#!/bin/bash -xe

gpg_pubkey=".travis/sixsq.gpg_pubkey.bin"
signed_file="code/resources/public/ui/downloads/nuvlabox-self-registration.py.gpg"

gpg --version
gpg --dry-run --batch --status-fd=1 --no-sig-cache --no-default-keyring --keyring "${gpg_pubkey}" --verify "${signed_file}"
